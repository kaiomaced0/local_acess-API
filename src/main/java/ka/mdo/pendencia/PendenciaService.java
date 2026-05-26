package ka.mdo.pendencia;

import ka.mdo.tenant.JwtClaims;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import ka.mdo.dto.PendenciaResponseDTO;
import ka.mdo.facial.FacialValidationService;
import ka.mdo.facial.PendenciaRequerida;
import ka.mdo.model.Aparelho;
import ka.mdo.model.DadosPessoais;
import ka.mdo.model.Empresa;
import ka.mdo.model.EspacoEvento;
import ka.mdo.model.Ingresso;
import ka.mdo.model.Pendencia;
import ka.mdo.model.Perfil;
import ka.mdo.model.StatusPendencia;
import ka.mdo.model.TipoNotificacao;
import ka.mdo.model.Usuario;
import ka.mdo.repository.AparelhoRepository;
import ka.mdo.repository.EmpresaRepository;
import ka.mdo.repository.EspacoEventoRepository;
import ka.mdo.repository.GestorLocalRepository;
import ka.mdo.repository.IngressoRepository;
import ka.mdo.repository.PendenciaRepository;
import ka.mdo.repository.UsuarioRepository;
import ka.mdo.service.NotificacaoService;
import ka.mdo.storage.StorageService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Orquestra o workflow de pendências de acesso (atividade 031).
 *
 * <h3>Criação</h3>
 * Dispara automaticamente quando o {@code AcessoService} emite
 * {@link PendenciaRequerida} (via CDI async observer). Idempotente: mesma
 * credencial + mesmo motivo com status {@link StatusPendencia#ABERTA} ⇒
 * atualizamos apenas a {@link Pendencia#setFotoCapturadaObjectKey(String)} e
 * retornamos. Não disparamos nova notificação (evita spam em retries rápidos
 * do aparelho).
 *
 * <h3>Notificação aos gestores</h3>
 * Hoje "gestor" = todo usuário da empresa cujo {@link Usuario#getPerfis()}
 * contém {@link Perfil#GESTOR_EVENTO} ou {@link Perfil#GESTOR_LOCAL}. Como o
 * vínculo específico gestor↔local ainda não existe, o leque fica largo — é
 * um débito conhecido da 041. A seção "Débitos" do arquivo da atividade 031
 * documenta isso.
 *
 * <h3>Resolução (aprovar/recusar)</h3>
 * <ul>
 *     <li>Idempotente: pendência já resolvida devolve o estado atual sem
 *     re-disparar notificação.</li>
 *     <li>{@code aprovar} de pendência de rosto divergente reseta
 *     {@link DadosPessoais#setRostoFrigateCadastrado(boolean)} para
 *     {@code false} — a próxima leitura re-enrola automaticamente, o que
 *     faz o caminho CADASTRADO → AUTORIZADO nas visitas seguintes.</li>
 *     <li>{@code aprovar} de pendência de dados pessoais é decisão pontual:
 *     libera aquele acesso naquele momento (o próprio listener do evento
 *     de auditoria já gravou o AUTORIZADO/PENDENTE). Próxima leitura sem
 *     dados volta a gerar pendência. A alternativa seria persistir uma
 *     flag {@code dadosPessoaisPendenciaLiberada} no {@code Ingresso},
 *     mas preferimos manter a semântica "evento exige = evento exige"
 *     — aprovação = uso único. Ver discussão no arquivo da atividade.</li>
 * </ul>
 *
 * <h3>Transações</h3>
 * O observer roda em {@link Transactional.TxType#REQUIRES_NEW} — falha ali
 * nunca volta para a thread do aparelho. Exceções são registradas via
 * {@code LOG.error} e não propagam.
 */
@ApplicationScoped
public class PendenciaService {

    private static final Logger LOG = Logger.getLogger(PendenciaService.class);

    @Inject
    PendenciaRepository pendenciaRepository;

    @Inject
    IngressoRepository ingressoRepository;

    @Inject
    EspacoEventoRepository espacoEventoRepository;

    @Inject
    AparelhoRepository aparelhoRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    NotificacaoService notificacaoService;

    @Inject
    GestorLocalRepository gestorLocalRepository;

    @Inject
    StorageService storageService;

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "storage.bucket.capturas-acesso", defaultValue = "capturas-acesso")
    String bucketCapturas;

    /**
     * Cria (ou atualiza, quando idempotente) uma pendência. Chamado tanto
     * pelo observer do {@link PendenciaRequerida} quanto pelo próprio
     * {@code AcessoService} quando decide cenários de {@code PENDENTE} que
     * não envolvem facial (DADOS_PESSOAIS_INCOMPLETOS).
     *
     * <p>Idempotência: se já existe uma {@link StatusPendencia#ABERTA} para
     * a mesma credencial e mesmo motivo, apenas atualizamos a foto capturada
     * e retornamos — <b>sem disparar nova notificação</b>.
     *
     * @param credencialId   id do {@link Ingresso}.
     * @param localId        id do {@link EspacoEvento} (pode ser null).
     * @param aparelhoId     id do {@link Aparelho}.
     * @param motivo         código curto (ex.: {@code ROSTO_DIVERGENTE}).
     * @param fotoObjectKey  chave da captura no bucket
     *                       {@code capturas-acesso} (pode ser null).
     */
    @Transactional
    public Pendencia criar(Long credencialId,
                           Long localId,
                           Long aparelhoId,
                           String motivo,
                           String fotoObjectKey) {
        if (credencialId == null || aparelhoId == null || motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "credencialId, aparelhoId e motivo são obrigatórios para abrir pendência");
        }

        Optional<Pendencia> existente = pendenciaRepository.findAbertaPorMotivo(credencialId, motivo);
        if (existente.isPresent()) {
            Pendencia p = existente.get();
            if (fotoObjectKey != null && !fotoObjectKey.isBlank()) {
                p.setFotoCapturadaObjectKey(fotoObjectKey);
            }
            LOG.infof("Pendência idempotente: atualizada foto de pendencia=%d credencial=%d motivo=%s",
                    p.getId(), credencialId, motivo);
            return p;
        }

        Ingresso credencial = ingressoRepository.findById(credencialId);
        if (credencial == null) {
            throw new NotFoundException("Credencial não encontrada: id=" + credencialId);
        }
        Aparelho aparelho = aparelhoRepository.findById(aparelhoId);
        if (aparelho == null) {
            throw new NotFoundException("Aparelho não encontrado: id=" + aparelhoId);
        }
        Empresa empresa = aparelho.getEmpresa() != null
                ? aparelho.getEmpresa()
                : credencial.getEmpresa();
        if (empresa == null) {
            throw new IllegalStateException(
                    "Não foi possível resolver empresa (tenant) para a pendência");
        }

        EspacoEvento local = localId != null ? espacoEventoRepository.findById(localId) : null;

        Pendencia p = new Pendencia();
        p.setEmpresa(empresa);
        p.setCredencial(credencial);
        p.setAparelho(aparelho);
        p.setLocal(local);
        p.setMotivo(motivo);
        p.setFotoCapturadaObjectKey(fotoObjectKey);
        p.setStatus(StatusPendencia.ABERTA);
        p.setCriadaEm(LocalDateTime.now());

        pendenciaRepository.persist(p);

        notificarGestores(empresa.getId(), p);
        return p;
    }

    /**
     * Observer assíncrono de {@link PendenciaRequerida}. Disparado pelo
     * {@code FacialValidationService} (ROSTO_DIVERGENTE, FRIGATE_INDISPONIVEL)
     * e pelo {@code AcessoService} (DADOS_PESSOAIS_INCOMPLETOS).
     *
     * <p>Roda em {@code REQUIRES_NEW} para que uma falha aqui não arraste a
     * thread original. Exceções viram {@code LOG.error} e não propagam.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void aoRequererPendencia(@ObservesAsync PendenciaRequerida evt) {
        try {
            criar(evt.ingressoId(), evt.localId(), evt.aparelhoId(),
                    evt.motivo(), evt.capturaObjectKey());
        } catch (RuntimeException e) {
            LOG.errorf(e, "Falha ao criar Pendencia a partir de PendenciaRequerida (evt=%s)", evt);
        }
    }

    /**
     * Aprova uma pendência. Idempotente: se já estava resolvida, devolve o
     * estado atual sem re-disparar notificação.
     *
     * <p>Efeito colateral para pendências faciais: reseta
     * {@link DadosPessoais#setRostoFrigateCadastrado(boolean)} para
     * {@code false}. A próxima leitura re-enrola o rosto no Frigate —
     * garantindo o critério E2E "nova leitura passa direto" sem cadastro
     * manual.
     */
    @Transactional
    public Pendencia aprovar(Long pendenciaId, String observacao) {
        return resolver(pendenciaId, StatusPendencia.APROVADA, observacao);
    }

    /**
     * Recusa a pendência. Idempotente.
     */
    @Transactional
    public Pendencia recusar(Long pendenciaId, String observacao) {
        return resolver(pendenciaId, StatusPendencia.RECUSADA, observacao);
    }

    private Pendencia resolver(Long pendenciaId, StatusPendencia novoStatus, String observacao) {
        if (pendenciaId == null) {
            throw new NotFoundException("Pendência não encontrada");
        }
        Pendencia p = pendenciaRepository.findById(pendenciaId);
        if (p == null) {
            throw new NotFoundException("Pendência não encontrada: id=" + pendenciaId);
        }

        // Tenant: confirmamos explicitamente (belt-and-suspenders além do
        // filtro Hibernate).
        Long empresaJwt = empresaDoJwtOuNull();
        if (empresaJwt != null
                && p.getEmpresa() != null
                && !p.getEmpresa().getId().equals(empresaJwt)) {
            throw new ForbiddenException("Pendência pertence a outro tenant");
        }

        // Idempotência: já resolvida → devolve como está, sem notificar de novo.
        if (p.getStatus() != StatusPendencia.ABERTA) {
            LOG.infof("Resolução idempotente: pendencia=%d já está %s — nada a fazer",
                    p.getId(), p.getStatus());
            return p;
        }

        p.setStatus(novoStatus);
        p.setResolvidaEm(LocalDateTime.now());
        p.setResolvidaPor(resolverUsuarioLogado());
        if (observacao != null && !observacao.isBlank()) {
            p.setObservacaoResolucao(observacao.length() > 500
                    ? observacao.substring(0, 500)
                    : observacao);
        }

        if (novoStatus == StatusPendencia.APROVADA) {
            aplicarEfeitosAprovacao(p);
        }

        notificarDono(p, novoStatus);
        return p;
    }

    /**
     * Efeitos colaterais da aprovação conforme o motivo da pendência.
     *
     * <ul>
     *     <li>{@code ROSTO_DIVERGENTE} / {@code FRIGATE_INDISPONIVEL}: reseta
     *     {@code rostoFrigateCadastrado=false} para re-enrolar na próxima
     *     leitura.</li>
     *     <li>{@code DADOS_PESSOAIS_INCOMPLETOS}: aprovação é uso único —
     *     o gestor decidiu liberar aquela leitura pontualmente. Próxima
     *     leitura sem dados volta a gerar pendência. Decisão documentada
     *     na atividade 031.</li>
     * </ul>
     */
    private void aplicarEfeitosAprovacao(Pendencia p) {
        if (p.getCredencial() == null) {
            return;
        }
        String motivo = p.getMotivo();
        boolean facial = motivo != null
                && (motivo.equals(FacialValidationService.MOTIVO_ROSTO_DIVERGENTE)
                    || motivo.equals(FacialValidationService.MOTIVO_FRIGATE_INDISPONIVEL));
        if (!facial) {
            return;
        }
        Usuario dono = usuarioRepository.findByIngressoId(p.getCredencial().getId());
        DadosPessoais dp = dono == null ? null : dono.getDadosPessoais();
        if (dp == null) {
            LOG.warnf("Pendencia %d aprovada, mas dono/DadosPessoais ausentes — sem reset do Frigate",
                    p.getId());
            return;
        }
        dp.setRostoFrigateCadastrado(false);
        LOG.infof("Pendencia %d aprovada: rostoFrigateCadastrado=false (re-enrolará na próxima leitura)",
                p.getId());
    }

    private void notificarGestores(Long empresaId, Pendencia p) {
        try {
            Long localId = p.getLocal() != null ? p.getLocal().getId() : null;
            List<Usuario> destinatarios = gestoresDaEmpresa(empresaId, localId);
            if (destinatarios.isEmpty()) {
                LOG.warnf("Pendencia %d criada mas nenhum gestor (GESTOR_EVENTO/GESTOR_LOCAL) "
                        + "encontrado para empresa=%d — notificação não disparada", p.getId(), empresaId);
                return;
            }
            String payload = String.format(
                    "{\"pendenciaId\":%d,\"credencialId\":%d,\"motivo\":\"%s\"}",
                    p.getId(), p.getCredencial().getId(), escapar(p.getMotivo()));
            String titulo = "Pendência aguardando revisão";
            String mensagem = "Uma credencial gerou pendência (" + p.getMotivo() + ") e precisa de análise.";
            for (Usuario gestor : destinatarios) {
                try {
                    notificacaoService.enviar(
                            gestor.getId(),
                            TipoNotificacao.PENDENCIA_ABERTA,
                            titulo,
                            mensagem,
                            payload);
                } catch (RuntimeException e) {
                    LOG.errorf(e, "Falha ao notificar gestor=%d sobre pendencia=%d",
                            gestor.getId(), p.getId());
                }
            }
        } catch (RuntimeException e) {
            LOG.errorf(e, "Falha ao resolver/notificar gestores (pendencia=%d)", p.getId());
        }
    }

    private void notificarDono(Pendencia p, StatusPendencia status) {
        try {
            if (p.getCredencial() == null) {
                return;
            }
            Usuario dono = usuarioRepository.findByIngressoId(p.getCredencial().getId());
            if (dono == null) {
                LOG.warnf("Pendencia %d resolvida mas credencial %d sem dono — notificação pulada",
                        p.getId(), p.getCredencial().getId());
                return;
            }
            TipoNotificacao tipo = status == StatusPendencia.APROVADA
                    ? TipoNotificacao.PENDENCIA_APROVADA
                    : TipoNotificacao.PENDENCIA_RECUSADA;
            String titulo = status == StatusPendencia.APROVADA
                    ? "Acesso aprovado"
                    : "Acesso recusado";
            String mensagem = status == StatusPendencia.APROVADA
                    ? "Sua pendência foi aprovada. Você já pode passar na próxima leitura."
                    : "Sua pendência foi recusada. Fale com a organização do evento.";
            String payload = String.format(
                    "{\"pendenciaId\":%d,\"credencialId\":%d,\"status\":\"%s\"}",
                    p.getId(), p.getCredencial().getId(), status.name());
            notificacaoService.enviar(dono.getId(), tipo, titulo, mensagem, payload);
        } catch (RuntimeException e) {
            LOG.errorf(e, "Falha ao notificar dono sobre resolução da pendencia=%d", p.getId());
        }
    }

    /**
     * Seleciona os usuários a serem notificados sobre uma pendência. Atividade
     * 041 fechou o débito: {@code GESTOR_EVENTO} continua recebendo todas as
     * pendências do tenant; {@code GESTOR_LOCAL} só é notificado sobre
     * pendências com {@code local} entre os seus vínculos
     * {@link ka.mdo.model.GestorLocal}. Pendências sem local (aparelho de
     * entrada geral) caem somente nos {@code GESTOR_EVENTO}.
     */
    private List<Usuario> gestoresDaEmpresa(Long empresaId, Long localDaPendencia) {
        if (empresaId == null) {
            return List.of();
        }
        // Sempre inclui GESTOR_EVENTO do tenant.
        String jpqlEvento = "SELECT DISTINCT u FROM Usuario u JOIN u.perfis pf "
                + "WHERE u.empresa.id = ?1 AND pf = ?2";
        List<Usuario> gestoresEvento = usuarioRepository.find(jpqlEvento,
                empresaId, Perfil.GESTOR_EVENTO).list();

        // GESTOR_LOCAL: só notifica quem gerencia o local da pendência.
        List<Usuario> gestoresLocal = List.of();
        if (localDaPendencia != null) {
            List<Long> idsVinculados = gestorLocalRepository.findGestoresDoLocal(localDaPendencia);
            if (!idsVinculados.isEmpty()) {
                gestoresLocal = usuarioRepository.list("id IN ?1", idsVinculados);
            }
        }

        // Dedup.
        Set<Long> vistos = new HashSet<>();
        List<Usuario> unicos = new ArrayList<>();
        for (Usuario u : gestoresEvento) {
            if (vistos.add(u.getId())) {
                unicos.add(u);
            }
        }
        for (Usuario u : gestoresLocal) {
            if (vistos.add(u.getId())) {
                unicos.add(u);
            }
        }
        return unicos;
    }

    private Long empresaDoJwtOuNull() {
        try {
            return JwtClaims.empresaIdOrNull(jwt);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Usuario resolverUsuarioLogado() {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            return null;
        }
        try {
            return usuarioRepository.findById(Long.parseLong(sub));
        } catch (NumberFormatException e) {
            LOG.warnf("subject do JWT não-numérico: %s", sub);
            return null;
        }
    }

    private static String escapar(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Converte para DTO de resposta — gera URL pré-assinada da foto sob
     * demanda e mascara o token da credencial.
     */
    public PendenciaResponseDTO toResponse(Pendencia p) {
        String tokenMascarado = null;
        String token = p.getCredencial() != null ? p.getCredencial().getToken() : null;
        if (token != null && !token.isBlank()) {
            String ultimos = token.length() <= 4
                    ? token
                    : token.substring(token.length() - 4);
            tokenMascarado = "***" + ultimos;
        }
        String url = null;
        if (p.getFotoCapturadaObjectKey() != null && !p.getFotoCapturadaObjectKey().isBlank()) {
            try {
                url = storageService.downloadUrl(bucketCapturas, p.getFotoCapturadaObjectKey());
            } catch (RuntimeException e) {
                LOG.warnf(e, "Falha ao assinar URL da captura (pendencia=%d)", p.getId());
            }
        }
        return new PendenciaResponseDTO(
                p.getId(),
                p.getCredencial() != null ? p.getCredencial().getId() : null,
                tokenMascarado,
                p.getLocal() != null ? p.getLocal().getId() : null,
                p.getAparelho() != null ? p.getAparelho().getId() : null,
                p.getMotivo(),
                url,
                p.getStatus(),
                p.getCriadaEm(),
                p.getResolvidaEm(),
                p.getResolvidaPor() != null ? p.getResolvidaPor().getId() : null
        );
    }

    /**
     * Busca paginada para o endpoint de fila. Delegado ao repositório — o
     * filtro {@code tenantFilter} do Hibernate isola por empresa.
     */
    public List<PendenciaResponseDTO> buscar(StatusPendencia status,
                                             Long localId,
                                             int pagina,
                                             int tamanho) {
        int p = Math.max(0, pagina);
        int t = tamanho <= 0 ? 20 : Math.min(tamanho, 200);
        Collection<Long> locais = locaisDoGestorLocalOuNull();
        return pendenciaRepository.buscar(status, localId, locais, p, t)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Atividade 041: quando o chamador é GESTOR_LOCAL (sem perfil acima),
     * restringe a visibilidade aos locais vinculados via
     * {@link ka.mdo.model.GestorLocal}. Fecha o débito 031.
     */
    public Collection<Long> locaisDoGestorLocalOuNull() {
        var groups = jwt.getGroups();
        if (groups == null) {
            return null;
        }
        boolean ehGestorLocal = groups.contains("GESTOR_LOCAL");
        boolean temPerfilAcima = groups.contains("SUPER_ADMIN")
                || groups.contains("ADMIN_EMPRESA")
                || groups.contains("GESTOR_EVENTO");
        if (!ehGestorLocal || temPerfilAcima) {
            return null;
        }
        Long usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) {
            return List.of();
        }
        return gestorLocalRepository.findLocaisDoGestor(usuarioId);
    }
}
