package ka.mdo.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.EmpresaDTO;
import ka.mdo.dto.EmpresaResponseDTO;
import ka.mdo.dto.StatusEmpresaDTO;
import ka.mdo.model.Empresa;
import ka.mdo.model.StatusEmpresa;
import ka.mdo.repository.EmpresaRepository;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class EmpresaService {

    /** Tamanho máximo de página aceito em {@link #listar(boolean, int, int)}. */
    private static final int PAGE_SIZE_MAX = 100;

    @Inject
    EmpresaRepository repository;

    @Transactional
    public Response insert(EmpresaDTO dto) {
        try {
            if (dto.cnpj() != null && repository.findByCnpj(dto.cnpj()) != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Já existe empresa com esse CNPJ").build();
            }
            Empresa empresa = new Empresa();
            empresa.setNome(dto.nome());
            empresa.setCnpj(dto.cnpj());
            empresa.setStatus(StatusEmpresa.ATIVA);
            repository.persist(empresa);
            Log.info("Empresa cadastrada: " + empresa.getId());
            return Response.status(Response.Status.CREATED)
                    .entity(new EmpresaResponseDTO(empresa)).build();
        } catch (Exception e) {
            Log.error("Erro ao cadastrar empresa: " + e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    /**
     * Listagem paginada (atividade 008). {@code pageSize} é clampeado em
     * {@value #PAGE_SIZE_MAX}.
     */
    public List<EmpresaResponseDTO> listar(boolean incluirInativas, int page, int size) {
        int pageIndex = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), PAGE_SIZE_MAX);
        return repository.listarPaginado(incluirInativas, pageIndex, pageSize)
                .stream()
                .map(EmpresaResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Busca por id (atividade 008). Retorna {@code null} quando não
     * encontrada ou quando soft-deleted e {@code incluirInativas == false}.
     */
    public EmpresaResponseDTO buscarPorId(Long id, boolean incluirInativas) {
        Empresa empresa = repository.findByIdConsiderandoAtivo(id, incluirInativas);
        return empresa == null ? null : new EmpresaResponseDTO(empresa);
    }

    /**
     * Atualiza dados cadastrais (atividade 008). NÃO atualiza
     * {@code status} — a transição vai por {@link #atualizarStatus}.
     *
     * @return resposta com a empresa atualizada ou 404/409.
     */
    @Transactional
    public Response atualizar(Long id, EmpresaDTO dto) {
        Empresa empresa = repository.findById(id);
        if (empresa == null || Boolean.FALSE.equals(empresa.getAtivo())) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        // Se o CNPJ mudou, garante unicidade
        if (dto.cnpj() != null && !dto.cnpj().equals(empresa.getCnpj())) {
            Empresa colidente = repository.findByCnpj(dto.cnpj());
            if (colidente != null && !colidente.getId().equals(id)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Já existe empresa com esse CNPJ").build();
            }
        }
        empresa.setNome(dto.nome());
        empresa.setCnpj(dto.cnpj());
        Log.info("Empresa atualizada: " + empresa.getId());
        return Response.ok(new EmpresaResponseDTO(empresa)).build();
    }

    /**
     * Transição explícita de {@link StatusEmpresa} (atividade 008).
     *
     * <p>Regras:
     * <ul>
     *   <li>{@code ATIVA} ↔ {@code SUSPENSA}</li>
     *   <li>{@code ATIVA} → {@code ENCERRADA}</li>
     *   <li>{@code SUSPENSA} → {@code ENCERRADA}</li>
     *   <li>{@code ENCERRADA} é estado final — qualquer transição é 409.</li>
     *   <li>Transição para o mesmo status é no-op (200).</li>
     * </ul>
     */
    @Transactional
    public Response atualizarStatus(Long id, StatusEmpresaDTO dto) {
        Empresa empresa = repository.findById(id);
        if (empresa == null || Boolean.FALSE.equals(empresa.getAtivo())) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        StatusEmpresa atual = empresa.getStatus();
        StatusEmpresa novo = dto.status();
        if (atual == novo) {
            return Response.ok(new EmpresaResponseDTO(empresa)).build();
        }
        if (!transicaoPermitida(atual, novo)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Transição de status inválida: " + atual + " -> " + novo)
                    .build();
        }
        empresa.setStatus(novo);
        Log.info("Empresa " + id + " status: " + atual + " -> " + novo);
        return Response.ok(new EmpresaResponseDTO(empresa)).build();
    }

    private boolean transicaoPermitida(StatusEmpresa atual, StatusEmpresa novo) {
        // ENCERRADA é absorvente.
        if (atual == StatusEmpresa.ENCERRADA) {
            return false;
        }
        // Todas as outras transições entre ATIVA/SUSPENSA/ENCERRADA são válidas.
        return true;
    }

    /**
     * Soft-delete (atividade 008). Marca {@code ativo=false}; não altera o
     * status. Operação idempotente.
     */
    @Transactional
    public Response softDelete(Long id) {
        Empresa empresa = repository.findById(id);
        if (empresa == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        empresa.setAtivo(false);
        Log.info("Empresa soft-deleted: " + id);
        return Response.noContent().build();
    }
}
