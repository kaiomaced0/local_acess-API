package ka.mdo.facial;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ka.mdo.frigate.FrigateException;
import ka.mdo.frigate.FrigateRostoMatch;
import ka.mdo.frigate.FrigateService;
import ka.mdo.model.DadosPessoais;
import ka.mdo.model.Ingresso;
import ka.mdo.model.Usuario;
import ka.mdo.repository.IngressoRepository;
import ka.mdo.repository.UsuarioRepository;
import ka.mdo.storage.StorageService;
import ka.mdo.storage.StorageValidator;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Orquestra a validação facial (atividade 021). Chamado pelo {@code AcessoService}
 * quando {@code Evento.validarFacial == true} e o aparelho trouxe a foto
 * capturada.
 *
 * <h3>Fluxo</h3>
 * <ol>
 *     <li>Persiste a foto capturada no bucket {@code capturas-acesso/}
 *     (sempre, para auditoria — independente do resultado).</li>
 *     <li>Resolve o dono da credencial e seus {@code DadosPessoais}.</li>
 *     <li>Se o dono ainda não tem rosto enrolado: chama
 *     {@link FrigateService#cadastrarRosto} com a foto capturada, marca
 *     {@code DadosPessoais.rostoFrigateCadastrado=true} e devolve
 *     {@link ResultadoFacial#CADASTRADO}.</li>
 *     <li>Se já tem: chama {@link FrigateService#compararRosto}. Score
 *     &ge; threshold → {@link ResultadoFacial#AUTORIZADO}. Caso contrário
 *     → {@link ResultadoFacial#DIVERGENTE} + disparo de
 *     {@link PendenciaRequerida} (consumido pela 031).</li>
 *     <li>Se o Frigate falhar em qualquer ponto (timeout/5xx): aplica a
 *     política de fallback {@code frigate.fallback}.</li>
 * </ol>
 *
 * <h3>Fallback</h3>
 * Configurável via {@code frigate.fallback}:
 * <ul>
 *     <li>{@code permitir} — devolve {@link ResultadoFacial#AUTORIZADO}
 *     (libera o acesso).</li>
 *     <li>{@code pendente} (default) — devolve {@link ResultadoFacial#INDISPONIVEL}
 *     e o caller escolhe entre {@code PENDENTE}.</li>
 *     <li>{@code negar} — devolve {@link ResultadoFacial#DIVERGENTE} (caller
 *     trata como {@code PENDENTE} com motivo distinto).</li>
 * </ul>
 *
 * <h3>Privacidade</h3>
 * Nunca logamos bytes; apenas {@code rostoCapturado.length}.
 */
@ApplicationScoped
public class FacialValidationService {

    private static final Logger LOG = Logger.getLogger(FacialValidationService.class);

    public static final String MOTIVO_ROSTO_DIVERGENTE = "ROSTO_DIVERGENTE";
    public static final String MOTIVO_FRIGATE_INDISPONIVEL = "FRIGATE_INDISPONIVEL";
    public static final String MOTIVO_SEM_DADOS_PESSOAIS = "SEM_DADOS_PESSOAIS_PARA_FACIAL";

    @Inject
    IngressoRepository ingressoRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    FrigateService frigateService;

    @Inject
    StorageService storageService;

    @Inject
    StorageValidator storageValidator;

    @Inject
    Event<PendenciaRequerida> pendenciaEvent;

    @ConfigProperty(name = "frigate.similaridade-minima", defaultValue = "0.85")
    double similaridadeMinima;

    @ConfigProperty(name = "frigate.fallback", defaultValue = "pendente")
    String fallback;

    @ConfigProperty(name = "storage.bucket.capturas-acesso", defaultValue = "capturas-acesso")
    String bucketCapturas;

    /**
     * Valida a foto capturada contra o rosto enrolado do dono da credencial.
     *
     * <p>Persiste as alterações em {@code DadosPessoais} (cadastro do rosto
     * via {@code @Transactional}); o upload do storage e chamadas HTTP
     * ocorrem antes do commit — se o Frigate falhar após o upload, a
     * captura fica no bucket mas a flag {@code rostoFrigateCadastrado} não
     * é marcada.
     *
     * @param ingressoId       id do ingresso que está sendo validado.
     * @param rostoCapturado   bytes da foto capturada pelo aparelho.
     * @param contentType      MIME real (ex: {@code image/jpeg}).
     * @param aparelhoId       id do aparelho (para disparar PendenciaRequerida).
     * @param localId          id do EspacoEvento (nullable).
     * @return {@link ResultadoFacialContexto} com a decisão + chave da captura.
     */
    @Transactional
    public ResultadoFacialContexto validar(Long ingressoId,
                                           byte[] rostoCapturado,
                                           String contentType,
                                           Long aparelhoId,
                                           Long localId) {
        storageValidator.validarImagem(rostoCapturado, contentType);

        Ingresso ingresso = ingressoRepository.findById(ingressoId);
        if (ingresso == null) {
            // Caller já deveria ter validado, mas defendemos em profundidade.
            return new ResultadoFacialContexto(
                    ResultadoFacial.DIVERGENTE, null, 0.0, MOTIVO_SEM_DADOS_PESSOAIS);
        }

        Long empresaId = ingresso.getEmpresa() != null ? ingresso.getEmpresa().getId() : null;
        String capturaKey = uploadCaptura(empresaId, ingressoId, rostoCapturado, contentType);

        Usuario dono = usuarioRepository.findByIngressoId(ingressoId);
        DadosPessoais dp = dono == null ? null : dono.getDadosPessoais();
        if (dp == null) {
            // Sem dados pessoais não dá para enrolar/comparar — retorna
            // divergente. Na prática, o passo de dados pessoais (020) já
            // teria devolvido PENDENTE antes se o evento exigisse dados.
            LOG.warnf("Facial: dono do ingresso %d sem DadosPessoais; tratando como DIVERGENTE",
                    ingressoId);
            dispararPendencia(empresaId, ingressoId, aparelhoId, localId,
                    capturaKey, MOTIVO_SEM_DADOS_PESSOAIS, 0.0);
            return new ResultadoFacialContexto(
                    ResultadoFacial.DIVERGENTE, capturaKey, 0.0, MOTIVO_SEM_DADOS_PESSOAIS);
        }

        String pessoaId = resolverPessoaId(dp, empresaId, dono.getId());

        try {
            if (!dp.isRostoFrigateCadastrado()) {
                // Primeira leitura: enrola o rosto capturado.
                frigateService.cadastrarRosto(pessoaId, rostoCapturado, contentType);
                dp.setFrigatePessoaId(pessoaId);
                dp.setRostoFrigateCadastrado(true);
                LOG.infof("Facial: rosto cadastrado no Frigate (ingresso=%d pessoaId=%s)",
                        ingressoId, pessoaId);
                return new ResultadoFacialContexto(
                        ResultadoFacial.CADASTRADO, capturaKey, 0.0, null);
            }

            // Já enrolado: comparar.
            FrigateRostoMatch match = frigateService.compararRosto(rostoCapturado, contentType);
            double score = match == null ? 0.0 : match.score();
            boolean mesmaPessoa = match != null
                    && match.pessoaId() != null
                    && match.pessoaId().equals(dp.getFrigatePessoaId());

            if (mesmaPessoa && score >= similaridadeMinima) {
                LOG.infof("Facial: AUTORIZADO (ingresso=%d score=%.2f)", ingressoId, score);
                return new ResultadoFacialContexto(
                        ResultadoFacial.AUTORIZADO, capturaKey, score, null);
            }

            LOG.warnf("Facial: DIVERGENTE (ingresso=%d score=%.2f matchPessoa=%s esperado=%s)",
                    ingressoId, score,
                    match == null ? "null" : match.pessoaId(),
                    dp.getFrigatePessoaId());
            dispararPendencia(empresaId, ingressoId, aparelhoId, localId,
                    capturaKey, MOTIVO_ROSTO_DIVERGENTE, score);
            return new ResultadoFacialContexto(
                    ResultadoFacial.DIVERGENTE, capturaKey, score, MOTIVO_ROSTO_DIVERGENTE);

        } catch (FrigateException e) {
            LOG.warnf(e, "Facial: Frigate indisponível (ingresso=%d) — aplicando fallback=%s",
                    ingressoId, fallback);
            return aplicarFallback(capturaKey, empresaId, ingressoId, aparelhoId, localId);
        }
    }

    private ResultadoFacialContexto aplicarFallback(String capturaKey,
                                                    Long empresaId,
                                                    Long ingressoId,
                                                    Long aparelhoId,
                                                    Long localId) {
        String normalizado = fallback == null ? "pendente" : fallback.trim().toLowerCase();
        switch (normalizado) {
            case "permitir":
                return new ResultadoFacialContexto(
                        ResultadoFacial.AUTORIZADO, capturaKey, 0.0, null);
            case "negar":
                dispararPendencia(empresaId, ingressoId, aparelhoId, localId,
                        capturaKey, MOTIVO_FRIGATE_INDISPONIVEL, 0.0);
                return new ResultadoFacialContexto(
                        ResultadoFacial.DIVERGENTE, capturaKey, 0.0, MOTIVO_FRIGATE_INDISPONIVEL);
            case "pendente":
            default:
                return new ResultadoFacialContexto(
                        ResultadoFacial.INDISPONIVEL, capturaKey, 0.0, MOTIVO_FRIGATE_INDISPONIVEL);
        }
    }

    /**
     * Persiste a captura no bucket {@code capturas-acesso/}. Se o upload
     * falhar, não propaga — segue com {@code null} na chave. A auditoria
     * perde a imagem, mas a decisão de acesso ainda rola (a 012 aceita
     * {@code fotoCapturadaUrl} null).
     */
    private String uploadCaptura(Long empresaId, Long ingressoId, byte[] bytes, String contentType) {
        String ext = contentType != null && contentType.contains("png") ? "png" : "jpg";
        String key = String.format("empresa/%d/ingresso/%d/%d.%s",
                empresaId == null ? 0 : empresaId,
                ingressoId,
                System.currentTimeMillis(),
                ext);
        try {
            return storageService.upload(bucketCapturas, key, bytes, contentType);
        } catch (RuntimeException e) {
            LOG.errorf(e, "Facial: falha ao salvar captura no storage (ingresso=%d bytes=%d)",
                    ingressoId, bytes.length);
            return null;
        }
    }

    private String resolverPessoaId(DadosPessoais dp, Long empresaId, Long usuarioId) {
        if (dp.getFrigatePessoaId() != null && !dp.getFrigatePessoaId().isBlank()) {
            return dp.getFrigatePessoaId();
        }
        // Derivação determinística: mesma pessoa em mesma empresa sempre
        // gera o mesmo id no Frigate; evita colisão entre tenants.
        return "empresa-" + empresaId + "-usuario-" + usuarioId;
    }

    private void dispararPendencia(Long empresaId,
                                   Long ingressoId,
                                   Long aparelhoId,
                                   Long localId,
                                   String capturaKey,
                                   String motivo,
                                   double score) {
        try {
            pendenciaEvent.fireAsync(new PendenciaRequerida(
                    empresaId, ingressoId, aparelhoId, localId, capturaKey, motivo, score));
        } catch (RuntimeException e) {
            LOG.errorf(e, "Falha ao disparar PendenciaRequerida (ingresso=%d motivo=%s)",
                    ingressoId, motivo);
        }
    }
}
