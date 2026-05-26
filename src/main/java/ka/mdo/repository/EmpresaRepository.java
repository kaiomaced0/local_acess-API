package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.Empresa;

import java.util.List;

/**
 * Repositório de {@code Empresa}. Como a entidade é o próprio tenant-root,
 * o filtro Hibernate {@code tenantFilter} NÃO se aplica aqui — todas as
 * queries operam globalmente. Acesso aos endpoints está restrito a
 * {@code SUPER_ADMIN} via {@code EmpresaResource}.
 */
@ApplicationScoped
public class EmpresaRepository implements PanacheRepository<Empresa> {

    public Empresa findByCnpj(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        return find("cnpj = ?1", cnpj).firstResult();
    }

    /**
     * Busca paginada (atividade 008).
     *
     * @param incluirInativas se {@code true}, inclui empresas com
     *                        {@code ativo = false} (soft-deleted) — usado
     *                        para auditoria.
     * @param pageIndex       índice da página (0-based).
     * @param pageSize        tamanho da página.
     */
    public List<Empresa> listarPaginado(boolean incluirInativas, int pageIndex, int pageSize) {
        String where = incluirInativas ? "1=1" : "ativo = true";
        return find(where, Sort.by("id").ascending())
                .page(Page.of(pageIndex, pageSize))
                .list();
    }

    /**
     * Busca por id respeitando soft-delete (atividade 008). Quando
     * {@code incluirInativas == false}, devolve {@code null} para empresas
     * com {@code ativo = false}.
     */
    public Empresa findByIdConsiderandoAtivo(Long id, boolean incluirInativas) {
        Empresa empresa = findById(id);
        if (empresa == null) {
            return null;
        }
        if (!incluirInativas && Boolean.FALSE.equals(empresa.getAtivo())) {
            return null;
        }
        return empresa;
    }
}
