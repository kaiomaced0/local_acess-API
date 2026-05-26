package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.Aparelho;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositório de {@link Aparelho}. O filtro multi-tenant do Hibernate isola
 * por empresa automaticamente — as consultas abaixo não recebem
 * {@code empresaId}.
 */
@ApplicationScoped
public class AparelhoRepository implements PanacheRepository<Aparelho> {

    /**
     * Lista paginada de aparelhos com filtros opcionais (atividade 014).
     *
     * <p>Filtros aplicados como predicados {@code AND}; quando o argumento é
     * {@code null} o predicado correspondente é omitido.
     *
     * @param ativo             true/false para filtrar por estado; null = todos.
     * @param eventoId          id do evento; null = sem filtro.
     * @param localEspecificoId id do espaço; null = sem filtro.
     * @param pageIndex         0-based.
     * @param pageSize          tamanho da página.
     */
    public List<Aparelho> listarFiltrado(Boolean ativo,
                                         Long eventoId,
                                         Long localEspecificoId,
                                         int pageIndex,
                                         int pageSize) {
        StringBuilder ql = new StringBuilder("1=1");
        List<Object> params = new ArrayList<>();
        int idx = 1;
        if (ativo != null) {
            ql.append(" AND ativo = ?").append(idx++);
            params.add(ativo);
        }
        if (eventoId != null) {
            ql.append(" AND evento.id = ?").append(idx++);
            params.add(eventoId);
        }
        if (localEspecificoId != null) {
            ql.append(" AND localEspecifico.id = ?").append(idx++);
            params.add(localEspecificoId);
        }
        return find(ql.toString(),
                Sort.by("id").descending(),
                params.toArray())
                .page(Page.of(pageIndex, pageSize))
                .list();
    }
}
