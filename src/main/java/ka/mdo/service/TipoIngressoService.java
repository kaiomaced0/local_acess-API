package ka.mdo.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.TipoIngressoDTO;
import ka.mdo.dto.TipoIngressoResponseDTO;
import ka.mdo.model.Empresa;
import ka.mdo.model.TipoIngresso;
import ka.mdo.repository.EmpresaRepository;
import ka.mdo.repository.IngressoRepository;
import ka.mdo.repository.TipoIngressoRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CRUD de {@link TipoIngresso} (atividade 015).
 *
 * <p>Regras principais:
 * <ul>
 *   <li>Todas as operações são isoladas por tenant (claim {@code empresaId}
 *       do JWT) — o tenantFilter do Hibernate já filtra leituras; o service
 *       garante o tenant correto em escritas e checagens cross-tenant em
 *       leituras por id.</li>
 *   <li>Nome do tipo de ingresso é único por empresa entre os ativos
 *       (pré-validado aqui + constraint {@code uk_tipoingresso_empresa_nome}
 *       no banco como rede de segurança contra concorrência).</li>
 *   <li>Soft-delete via {@code ativo=false}; rejeita a exclusão (409) se
 *       há {@link ka.mdo.model.Ingresso} ativo referenciando o tipo.</li>
 * </ul>
 */
@ApplicationScoped
public class TipoIngressoService {

    @Inject
    TipoIngressoRepository repository;

    @Inject
    IngressoRepository ingressoRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    JsonWebToken jwt;

    private Long empresaDoJwt() {
        Long empresaId = jwt.getClaim("empresaId");
        if (empresaId == null) {
            throw new ForbiddenException("JWT sem empresaId");
        }
        return empresaId;
    }

    /**
     * Garante que o registro encontrado pertence ao tenant do JWT. O
     * tenantFilter já restringe leituras para perfis não-SUPER_ADMIN, mas
     * SUPER_ADMIN ignora o filtro — então fazemos a checagem explícita
     * (com possibilidade de no-op se o JWT for SUPER_ADMIN sem empresaId).
     */
    private void validarMesmoTenant(TipoIngresso tipo) {
        Long empresaId = jwt.getClaim("empresaId");
        if (empresaId == null) {
            // SUPER_ADMIN sem empresaId — não filtra (cross-tenant permitido).
            return;
        }
        if (tipo == null || tipo.getEmpresa() == null
                || !tipo.getEmpresa().getId().equals(empresaId)) {
            throw new ForbiddenException("TipoIngresso pertence a outra empresa");
        }
    }

    public List<TipoIngressoResponseDTO> listar(boolean incluirInativos, int pagina, int tamanho) {
        int p = Math.max(0, pagina);
        int t = tamanho <= 0 ? 20 : Math.min(tamanho, 200);
        return repository.listarDoTenant(incluirInativos, p, t)
                .stream()
                .map(TipoIngressoResponseDTO::new)
                .collect(Collectors.toList());
    }

    public TipoIngressoResponseDTO buscarPorId(Long id) {
        TipoIngresso tipo = repository.findById(id);
        if (tipo == null) {
            throw new NotFoundException("TipoIngresso não encontrado");
        }
        validarMesmoTenant(tipo);
        return new TipoIngressoResponseDTO(tipo);
    }

    @Transactional
    public Response criar(TipoIngressoDTO dto) {
        Long empresaId = empresaDoJwt();
        Empresa empresa = empresaRepository.findById(empresaId);
        if (empresa == null) {
            throw new ForbiddenException("Empresa do JWT não encontrada");
        }

        TipoIngresso existente = repository.findAtivoByNomeExato(dto.nome(), null);
        if (existente != null) {
            throw new WebApplicationException(
                    "Já existe TipoIngresso ativo com esse nome na empresa",
                    Response.Status.CONFLICT);
        }

        TipoIngresso tipo = new TipoIngresso();
        tipo.setNome(dto.nome());
        tipo.setEmpresa(empresa);
        repository.persist(tipo);
        Log.infof("TipoIngresso criado: id=%d empresaId=%d", tipo.getId(), empresaId);
        return Response.status(Response.Status.CREATED)
                .entity(new TipoIngressoResponseDTO(tipo))
                .build();
    }

    @Transactional
    public TipoIngressoResponseDTO atualizar(Long id, TipoIngressoDTO dto) {
        TipoIngresso tipo = repository.findById(id);
        if (tipo == null) {
            throw new NotFoundException("TipoIngresso não encontrado");
        }
        validarMesmoTenant(tipo);

        TipoIngresso conflito = repository.findAtivoByNomeExato(dto.nome(), id);
        if (conflito != null) {
            throw new WebApplicationException(
                    "Já existe outro TipoIngresso ativo com esse nome na empresa",
                    Response.Status.CONFLICT);
        }

        tipo.setNome(dto.nome());
        Log.infof("TipoIngresso atualizado: id=%d", id);
        return new TipoIngressoResponseDTO(tipo);
    }

    /**
     * Soft-delete. Bloqueia (409) se ainda há {@link ka.mdo.model.Ingresso}
     * ativo apontando para esse tipo — caso contrário, credenciais já
     * emitidas ficariam órfãs em queries que dependem do tipo.
     */
    @Transactional
    public void deletar(Long id) {
        TipoIngresso tipo = repository.findById(id);
        if (tipo == null) {
            throw new NotFoundException("TipoIngresso não encontrado");
        }
        validarMesmoTenant(tipo);

        long ingressosAtivos = ingressoRepository.count(
                "tipoIngresso.id = ?1 AND ativo = true", id);
        if (ingressosAtivos > 0) {
            throw new WebApplicationException(
                    "Não é possível excluir: existem " + ingressosAtivos
                            + " credenciais ativas vinculadas a este TipoIngresso",
                    Response.Status.CONFLICT);
        }

        tipo.setAtivo(false);
        Log.infof("TipoIngresso soft-deletado: id=%d", id);
    }
}
