package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.TipoIngresso;

import java.util.List;

/**
 * Repositório de {@link TipoIngresso}. O filtro multi-tenant do Hibernate
 * isola por empresa automaticamente — nenhuma das queries abaixo recebe
 * {@code empresaId} explicitamente.
 */
@ApplicationScoped
public class TipoIngressoRepository implements PanacheRepository<TipoIngresso> {

    public List<TipoIngresso> findByNome(String nome) {
        if (nome == null)
            return null;
        return find("UPPER(nome) LIKE ?1 ", "%" + nome.toUpperCase() + "%").list();
    }

    /**
     * Atividade 015: busca um tipo de ingresso pelo nome exato dentro do
     * tenant atual (tenantFilter já aplicado). Usado para pré-validar a
     * unicidade do nome antes de persistir/atualizar — a constraint do
     * banco (uk_tipoingresso_empresa_nome, V16) é a rede de segurança
     * contra condição de corrida.
     *
     * @param nome      nome exato (case-sensitive); {@code null} retorna {@code null}
     * @param excluirId opcional — id a desconsiderar (útil em update; pode ser {@code null})
     */
    public TipoIngresso findAtivoByNomeExato(String nome, Long excluirId) {
        if (nome == null) {
            return null;
        }
        if (excluirId == null) {
            return find("nome = ?1 AND ativo = true", nome).firstResult();
        }
        return find("nome = ?1 AND ativo = true AND id <> ?2", nome, excluirId).firstResult();
    }

    /**
     * Atividade 015: listagem paginada do tenant. Quando
     * {@code incluirInativos=false}, traz apenas os tipos com
     * {@code ativo=true} (default no painel). Ordenação por id ascendente
     * para garantir resultado estável entre páginas.
     */
    public List<TipoIngresso> listarDoTenant(boolean incluirInativos, int pageIndex, int pageSize) {
        Sort sort = Sort.by("id");
        if (incluirInativos) {
            return findAll(sort).page(Page.of(pageIndex, pageSize)).list();
        }
        return find("ativo = true", sort).page(Page.of(pageIndex, pageSize)).list();
    }
}
