package ka.mdo.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import ka.mdo.dto.AutorizacaoResponseDTO;
import ka.mdo.model.AcaoAutorizacao;
import ka.mdo.model.AutorizacaoAuditoria;
import ka.mdo.model.EspacoEvento;
import ka.mdo.model.TipoIngresso;
import ka.mdo.repository.AutorizacaoAuditoriaRepository;
import ka.mdo.repository.EspacoEventoRepository;
import ka.mdo.repository.GestorLocalRepository;
import ka.mdo.repository.TipoIngressoRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Regras de negócio da whitelist de {@link TipoIngresso} autorizados por
 * {@link EspacoEvento} (atividade 030).
 *
 * <p><b>Decisão de modelagem</b>: reutilizamos {@link TipoIngresso} em vez de
 * introduzir um novo {@code PerfilCredencial}. {@code TipoIngresso} já existe,
 * já é multitenant, e toda {@code Credencial}/Ingresso já referencia um. Criar
 * um segundo conceito agora seria complexidade antecipada — quando a 041 (ou
 * similar) precisar de granularidade extra (staff/cortesia/VIP cross-tipo),
 * modelamos perfis; por ora, "tipo" serve como proxy.
 *
 * <p><b>Auditoria</b>: toda mudança grava {@link AutorizacaoAuditoria} na
 * mesma transação. Sem cache — {@code AcessoService} lê a whitelist direto do
 * banco a cada validação. Otimização fica para a atividade 041.
 *
 * <p><b>Multitenancy</b>: toda operação checa que o {@code empresaId} do JWT
 * bate com o {@code empresa_id} do {@link EspacoEvento} alvo e de cada
 * {@link TipoIngresso} referenciado. 403 caso contrário.
 */
@ApplicationScoped
public class AutorizacaoEspacoService {

    private static final Logger LOG = Logger.getLogger(AutorizacaoEspacoService.class);

    @Inject
    EspacoEventoRepository espacoEventoRepository;

    @Inject
    TipoIngressoRepository tipoIngressoRepository;

    @Inject
    AutorizacaoAuditoriaRepository auditoriaRepository;

    @Inject
    GestorLocalRepository gestorLocalRepository;

    @Inject
    JsonWebToken jwt;

    private Long empresaDoJwt() {
        Long empresaId = jwt.getClaim("empresaId");
        if (empresaId == null) {
            throw new ForbiddenException("JWT sem empresaId");
        }
        return empresaId;
    }

    /** Extrai o id do usuário que fez a mudança (claim {@code usuarioId}). */
    private Long usuarioDoJwt() {
        Long usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) {
            throw new ForbiddenException("JWT sem usuarioId");
        }
        return usuarioId;
    }

    private EspacoEvento carregarEspacoDoTenant(Long espacoId) {
        EspacoEvento espaco = espacoEventoRepository.findById(espacoId);
        if (espaco == null) {
            throw new NotFoundException("EspacoEvento " + espacoId + " não encontrado");
        }
        if (espaco.getEmpresa() == null
                || !espaco.getEmpresa().getId().equals(empresaDoJwt())) {
            throw new ForbiddenException("EspacoEvento pertence a outra empresa");
        }
        // Atividade 041: GESTOR_LOCAL só pode operar sobre locais vinculados.
        var groups = jwt.getGroups();
        boolean ehGestorLocal = groups != null && groups.contains("GESTOR_LOCAL");
        boolean temPerfilAcima = groups != null && (groups.contains("SUPER_ADMIN")
                || groups.contains("ADMIN_EMPRESA")
                || groups.contains("GESTOR_EVENTO"));
        if (ehGestorLocal && !temPerfilAcima) {
            Long usuarioId = jwt.getClaim("usuarioId");
            java.util.List<Long> vinculados = usuarioId == null
                    ? java.util.List.of()
                    : gestorLocalRepository.findLocaisDoGestor(usuarioId);
            if (!vinculados.contains(espaco.getId())) {
                throw new ForbiddenException(
                        "GESTOR_LOCAL sem vínculo ao EspacoEvento " + espaco.getId());
            }
        }
        return espaco;
    }

    private TipoIngresso carregarTipoDoTenant(Long tipoIngressoId) {
        TipoIngresso tipo = tipoIngressoRepository.findById(tipoIngressoId);
        if (tipo == null) {
            throw new NotFoundException("TipoIngresso " + tipoIngressoId + " não encontrado");
        }
        if (tipo.getEmpresa() == null
                || !tipo.getEmpresa().getId().equals(empresaDoJwt())) {
            throw new ForbiddenException("TipoIngresso pertence a outra empresa");
        }
        return tipo;
    }

    public AutorizacaoResponseDTO listar(Long espacoId) {
        EspacoEvento espaco = carregarEspacoDoTenant(espacoId);
        // Força inicialização da coleção LAZY dentro da transação da request.
        Set<TipoIngresso> tipos = new LinkedHashSet<>(espaco.getTiposIngressoAutorizados());
        return AutorizacaoResponseDTO.from(espaco, tipos);
    }

    @Transactional
    public AutorizacaoResponseDTO adicionar(Long espacoId, Long tipoIngressoId) {
        if (tipoIngressoId == null) {
            throw new BadRequestException("tipoIngressoId obrigatório");
        }
        EspacoEvento espaco = carregarEspacoDoTenant(espacoId);
        TipoIngresso tipo = carregarTipoDoTenant(tipoIngressoId);

        boolean adicionado = espaco.getTiposIngressoAutorizados().add(tipo);
        if (adicionado) {
            registrarAuditoria(espaco, AcaoAutorizacao.ADICIONADO, tipo.getId());
            LOG.infof("Autorização ADICIONADA espaco=%d tipoIngresso=%d por usuario=%d",
                    espaco.getId(), tipo.getId(), usuarioDoJwt());
        } else {
            LOG.debugf("Autorização já existente (no-op) espaco=%d tipoIngresso=%d",
                    espaco.getId(), tipo.getId());
        }
        return AutorizacaoResponseDTO.from(espaco,
                new LinkedHashSet<>(espaco.getTiposIngressoAutorizados()));
    }

    @Transactional
    public AutorizacaoResponseDTO remover(Long espacoId, Long tipoIngressoId) {
        if (tipoIngressoId == null) {
            throw new BadRequestException("tipoIngressoId obrigatório");
        }
        EspacoEvento espaco = carregarEspacoDoTenant(espacoId);
        // Não precisamos validar o tenant do TipoIngresso aqui — se ele não
        // está na coleção do espaço, o remove é no-op. Mas para consistência
        // (ex.: id inválido vira 404), validamos mesmo assim.
        TipoIngresso tipo = carregarTipoDoTenant(tipoIngressoId);

        boolean removido = espaco.getTiposIngressoAutorizados()
                .removeIf(t -> t.getId().equals(tipo.getId()));
        if (removido) {
            registrarAuditoria(espaco, AcaoAutorizacao.REMOVIDO, tipo.getId());
            LOG.infof("Autorização REMOVIDA espaco=%d tipoIngresso=%d por usuario=%d",
                    espaco.getId(), tipo.getId(), usuarioDoJwt());
        } else {
            LOG.debugf("Autorização inexistente (no-op) espaco=%d tipoIngresso=%d",
                    espaco.getId(), tipo.getId());
        }
        return AutorizacaoResponseDTO.from(espaco,
                new LinkedHashSet<>(espaco.getTiposIngressoAutorizados()));
    }

    @Transactional
    public AutorizacaoResponseDTO substituir(Long espacoId, Set<Long> tiposIngressoIds) {
        if (tiposIngressoIds == null) {
            throw new BadRequestException("tiposIngressoIds obrigatório (use [] para limpar)");
        }
        EspacoEvento espaco = carregarEspacoDoTenant(espacoId);

        // Valida todos os ids e carrega — falha atômica se qualquer id for
        // inválido ou de outro tenant.
        Set<TipoIngresso> novos = new HashSet<>();
        for (Long id : tiposIngressoIds) {
            if (id == null) {
                throw new BadRequestException("tiposIngressoIds contém null");
            }
            novos.add(carregarTipoDoTenant(id));
        }

        espaco.getTiposIngressoAutorizados().clear();
        espaco.getTiposIngressoAutorizados().addAll(novos);

        registrarAuditoria(espaco, AcaoAutorizacao.SUBSTITUIDO, null);
        LOG.infof("Autorização SUBSTITUIDA espaco=%d qtd=%d por usuario=%d",
                espaco.getId(), novos.size(), usuarioDoJwt());

        return AutorizacaoResponseDTO.from(espaco,
                new LinkedHashSet<>(espaco.getTiposIngressoAutorizados()));
    }

    private void registrarAuditoria(EspacoEvento espaco,
                                    AcaoAutorizacao acao,
                                    Long tipoIngressoId) {
        AutorizacaoAuditoria log = new AutorizacaoAuditoria();
        log.setEmpresa(espaco.getEmpresa());
        log.setEspaco(espaco);
        log.setAcao(acao);
        log.setTipoIngressoId(tipoIngressoId);
        log.setUsuarioId(usuarioDoJwt());
        log.setDataHora(LocalDateTime.now());
        auditoriaRepository.persist(log);
    }
}
