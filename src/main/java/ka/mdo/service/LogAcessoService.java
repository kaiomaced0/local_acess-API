package ka.mdo.service;

import ka.mdo.tenant.JwtClaims;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import ka.mdo.dto.LogAcessoResponseDTO;
import ka.mdo.model.Aparelho;
import ka.mdo.model.Empresa;
import ka.mdo.model.EspacoEvento;
import ka.mdo.model.Ingresso;
import ka.mdo.model.LogAcesso;
import ka.mdo.model.TipoMovimento;
import ka.mdo.repository.AparelhoRepository;
import ka.mdo.repository.EmpresaRepository;
import ka.mdo.repository.EspacoEventoRepository;
import ka.mdo.repository.GestorLocalRepository;
import ka.mdo.repository.IngressoRepository;
import ka.mdo.repository.LogAcessoRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Persiste {@link LogAcesso} de forma assíncrona e responde às consultas de
 * auditoria ({@code GET /logs-acesso}).
 *
 * <p>Estratégia de gravação: CDI {@code @ObservesAsync} sobre
 * {@link AcessoOcorrido}. Disparado por {@code AcessoService#validar} via
 * {@code Event#fireAsync}, a chamada retorna imediatamente e o listener roda
 * em outra thread, em sua própria transação ({@link Transactional.TxType#REQUIRES_NEW}).
 * Se o listener falhar, apenas logamos — a resposta ao aparelho já foi
 * entregue e nunca é bloqueada pela auditoria.
 *
 * <p>TODO futuro: particionar {@code LogAcesso} por mês se o volume exigir.
 */
@ApplicationScoped
public class LogAcessoService {

    private static final Logger LOG = Logger.getLogger(LogAcessoService.class);

    @Inject
    LogAcessoRepository logAcessoRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    AparelhoRepository aparelhoRepository;

    @Inject
    IngressoRepository ingressoRepository;

    @Inject
    EspacoEventoRepository espacoEventoRepository;

    @Inject
    GestorLocalRepository gestorLocalRepository;

    @Inject
    JsonWebToken jwt;

    /**
     * Listener assíncrono do evento {@link AcessoOcorrido}. Roda fora da
     * transação do request original ({@code REQUIRES_NEW}) para que um
     * rollback na validação não leve o log junto — e vice-versa: falha na
     * gravação do log nunca propaga para o chamador original, apenas vira
     * um {@code LOG.error}.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void registrar(@ObservesAsync AcessoOcorrido evt) {
        try {
            Empresa empresa = empresaRepository.findById(evt.empresaId());
            if (empresa == null) {
                LOG.errorf("Falha ao registrar LogAcesso: empresa %d inexistente (evt=%s)",
                        evt.empresaId(), evt);
                return;
            }

            // aparelho é NOT NULL: se sumir entre a decisão e o log, registramos erro
            // e abandonamos — não queremos driblar a constraint com um valor sintético.
            Aparelho aparelho = aparelhoRepository.findById(evt.aparelhoId());
            if (aparelho == null) {
                LOG.errorf("Falha ao registrar LogAcesso: aparelho %d não encontrado (evt=%s)",
                        evt.aparelhoId(), evt);
                return;
            }

            Ingresso ingresso = null;
            if (evt.ingressoId() != null) {
                ingresso = ingressoRepository.findById(evt.ingressoId());
            }

            EspacoEvento local = null;
            if (evt.localId() != null) {
                local = espacoEventoRepository.findById(evt.localId());
            }

            LogAcesso log = new LogAcesso();
            log.setEmpresa(empresa);
            log.setAparelho(aparelho);
            log.setIngresso(ingresso);
            log.setLocal(local);
            log.setResultado(evt.resultado());
            log.setMotivo(evt.motivo());
            log.setDataHora(evt.dataHora() != null ? evt.dataHora() : LocalDateTime.now());
            // Atividade 021: persiste a chave da foto capturada (bucket
            // `capturas-acesso`). A URL assinada é gerada on-demand pelo
            // endpoint de leitura de logs. Pode ser null (fluxo sem foto).
            log.setFotoCapturadaUrl(evt.fotoCapturadaObjectKey());
            // Atividade 033: destaca no log que a decisão passou pelo
            // curto-circuito de credencial global.
            log.setAcessoGlobal(evt.acessoGlobal());
            // Atividade 041: tipoMovimento (ENTRADA|SAIDA).
            log.setTipoMovimento(
                    evt.tipoMovimento() == null ? TipoMovimento.ENTRADA : evt.tipoMovimento());

            logAcessoRepository.persist(log);
        } catch (RuntimeException e) {
            // NUNCA propagar: gravação de auditoria não pode quebrar o happy path
            // (neste ponto o happy path já retornou de qualquer forma, mas o
            // contrato é explícito). Também não logamos o token da credencial.
            LOG.errorf(e, "Falha ao registrar LogAcesso (evt=%s)", e.getMessage());
        }
    }

    /**
     * Consulta paginada usada pelo endpoint de auditoria. O filtro
     * {@code tenantFilter} do Hibernate (ativado pelo {@code TenantRequestFilter})
     * já limita os resultados à empresa do JWT — esta camada não precisa
     * repetir a cláusula {@code empresa_id = :empresaId}.
     *
     * <p>Atividade 041: se o chamador é {@code GESTOR_LOCAL} (e não
     * acumula um perfil acima), restringe automaticamente aos locais
     * vinculados via {@link ka.mdo.model.GestorLocal}.
     */
    public List<LogAcessoResponseDTO> buscar(Long credencialId,
                                             Long localId,
                                             LocalDateTime de,
                                             LocalDateTime ate,
                                             int pagina,
                                             int tamanho) {
        empresaDoJwt(); // 403 cedo se JWT não traz empresaId (coerente com o restante do app)

        int pageIndex = Math.max(0, pagina);
        int pageSize = tamanho <= 0 ? 20 : Math.min(tamanho, 200);

        Collection<Long> locaisPermitidos = locaisDoGestorLocalOuNull();

        List<LogAcesso> logs = logAcessoRepository.buscar(
                credencialId, localId, de, ate, locaisPermitidos, pageIndex, pageSize);

        return logs.stream().map(this::toResponse).toList();
    }

    /**
     * Se o JWT traz EXCLUSIVAMENTE {@code GESTOR_LOCAL} (sem outro perfil
     * "maior"), devolve a lista de ids de locais vinculados — restringe a
     * visibilidade. Para os demais perfis devolve {@code null} (sem
     * restrição adicional; o filtro por tenant do Hibernate já cuida).
     */
    private Collection<Long> locaisDoGestorLocalOuNull() {
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
            return List.of(); // gestor sem usuarioId claim ⇒ nenhuma visibilidade
        }
        return gestorLocalRepository.findLocaisDoGestor(usuarioId);
    }

    private Long empresaDoJwt() {
        Long empresaId = JwtClaims.empresaIdOrNull(jwt);
        if (empresaId == null) {
            throw new ForbiddenException("JWT sem empresaId");
        }
        return empresaId;
    }

    private LogAcessoResponseDTO toResponse(LogAcesso log) {
        return new LogAcessoResponseDTO(
                log.getId(),
                log.getIngresso() != null ? log.getIngresso().getId() : null,
                log.getLocal() != null ? log.getLocal().getId() : null,
                log.getAparelho() != null ? log.getAparelho().getId() : null,
                log.getAparelho() != null ? log.getAparelho().getDescricao() : null,
                log.getResultado(),
                log.getMotivo(),
                log.getDataHora(),
                log.getFotoCapturadaUrl(),
                log.isAcessoGlobal()
        );
    }
}
