package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.LogAcesso;
import ka.mdo.model.ResultadoAcesso;
import ka.mdo.model.TipoMovimento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositório de {@link LogAcesso}. O filtro multi-tenant do Hibernate
 * (sessão ativa) isola por empresa automaticamente — as queries abaixo não
 * precisam (e não devem) confiar em {@code empresaId} vindo do cliente.
 */
@ApplicationScoped
public class LogAcessoRepository implements PanacheRepository<LogAcesso> {

    /**
     * Busca paginada com filtros opcionais. Todos os argumentos podem ser
     * {@code null}; nesse caso o filtro correspondente é omitido da query.
     *
     * <p>Ordenação: mais recentes primeiro ({@code dataHora DESC}).
     *
     * @param credencialId    id do {@code Ingresso} referenciado no log (opcional).
     * @param localId         id do {@code EspacoEvento} (opcional).
     * @param de              data inicial (inclusive) — opcional.
     * @param ate             data final (inclusive) — opcional.
     * @param locaisPermitidos se {@code != null}, restringe a esses locais
     *                         (usado quando chamador é {@code GESTOR_LOCAL}).
     *                         Coleção vazia ⇒ nenhum resultado (1=0).
     * @param pageIndex       índice da página (0-based).
     * @param pageSize        tamanho da página.
     */
    public List<LogAcesso> buscar(Long credencialId,
                                  Long localId,
                                  LocalDateTime de,
                                  LocalDateTime ate,
                                  Collection<Long> locaisPermitidos,
                                  int pageIndex,
                                  int pageSize) {
        List<String> clauses = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (credencialId != null) {
            clauses.add("ingresso.id = :credencialId");
            params.put("credencialId", credencialId);
        }
        if (localId != null) {
            clauses.add("local.id = :localId");
            params.put("localId", localId);
        }
        if (de != null) {
            clauses.add("dataHora >= :de");
            params.put("de", de);
        }
        if (ate != null) {
            clauses.add("dataHora <= :ate");
            params.put("ate", ate);
        }
        if (locaisPermitidos != null) {
            if (locaisPermitidos.isEmpty()) {
                // Gestor de local sem vínculos: ninguém vê nada.
                return List.of();
            }
            clauses.add("local.id IN (:locaisPermitidos)");
            params.put("locaisPermitidos", locaisPermitidos);
        }

        String where = clauses.isEmpty() ? "1=1" : String.join(" AND ", clauses);

        return find(where, Sort.by("dataHora").descending(), params)
                .page(Page.of(pageIndex, pageSize))
                .list();
    }

    /**
     * Ocupação por local do evento (atividade 041).
     *
     * <p>Calculada como
     * {@code COUNT(ENTRADA) - COUNT(SAIDA)} para {@code resultado=AUTORIZADO},
     * agrupada por {@code local}. Retorna {@code Object[]{localId, localNome, ocupacao}}.
     *
     * <p>O {@code evento_id} vem de {@code EspacoEvento ↔ Evento} via a FK
     * {@code evento_espacoevento} (lado Evento). Juntamos via Evento para
     * filtrar somente locais do evento requisitado — e ignoramos logs cujo
     * local é {@code null} (entrada geral).
     *
     * @param eventoId        evento alvo.
     * @param locaisPermitidos quando {@code != null}, restringe aos ids
     *                         listados (visão do GESTOR_LOCAL). Coleção
     *                         vazia ⇒ nenhum resultado.
     */
    public List<Object[]> ocupacaoPorLocal(Long eventoId, Collection<Long> locaisPermitidos) {
        if (locaisPermitidos != null && locaisPermitidos.isEmpty()) {
            return List.of();
        }
        // Uso um subselect para amarrar os locais ao evento — compatível com
        // MariaDB e Postgres (não depende de ON MEMBER OF).
        StringBuilder jpql = new StringBuilder()
                .append("SELECT l.id, l.nome, ")
                .append("       SUM(CASE WHEN la.tipoMovimento = :entrada THEN 1 ELSE 0 END) - ")
                .append("       SUM(CASE WHEN la.tipoMovimento = :saida  THEN 1 ELSE 0 END) ")
                .append("FROM LogAcesso la ")
                .append("JOIN la.local l ")
                .append("WHERE la.resultado = :autorizado ")
                .append("AND l.id IN (SELECT es.id FROM Evento e JOIN e.espacoEventos es ")
                .append("              WHERE e.id = :eventoId) ");
        if (locaisPermitidos != null) {
            jpql.append("AND l.id IN (:locaisPermitidos) ");
        }
        jpql.append("GROUP BY l.id, l.nome");

        var query = getEntityManager().createQuery(jpql.toString(), Object[].class)
                .setParameter("entrada", TipoMovimento.ENTRADA)
                .setParameter("saida", TipoMovimento.SAIDA)
                .setParameter("autorizado", ResultadoAcesso.AUTORIZADO)
                .setParameter("eventoId", eventoId);
        if (locaisPermitidos != null) {
            query.setParameter("locaisPermitidos", locaisPermitidos);
        }
        return query.getResultList();
    }

    /**
     * Busca bruta de entradas autorizadas entre {@code de} e {@code ate} para
     * o evento. Retorna apenas os {@link LogAcesso#getDataHora()}.
     *
     * <p><b>Decisão (041): agregação por hora é feita em Java, não em SQL.</b>
     * {@code DATE_TRUNC('hour', ...)} existe no Postgres mas não no MariaDB,
     * e converter para {@code DATE_FORMAT}/{@code TIMESTAMPDIFF} quebra a
     * portabilidade. Como o endpoint limita o range a 7 dias (168 horas) e
     * o volume típico por hora raramente ultrapassa dezenas de milhares, o
     * custo de trazer as linhas e agrupar no aplicativo é aceitável e o
     * código fica trivialmente portável.
     *
     * @param eventoId        evento alvo.
     * @param de              data inicial (inclusive).
     * @param ate             data final (exclusiva no caller para evitar dupla contagem).
     * @param locaisPermitidos quando {@code != null}, restringe aos ids
     *                         listados (visão do GESTOR_LOCAL). Coleção
     *                         vazia ⇒ nenhum resultado.
     */
    public List<LocalDateTime> entradasAutorizadasNoPeriodo(Long eventoId,
                                                            LocalDateTime de,
                                                            LocalDateTime ate,
                                                            Collection<Long> locaisPermitidos) {
        if (locaisPermitidos != null && locaisPermitidos.isEmpty()) {
            return List.of();
        }
        // Usamos subselect (ao invés de EXISTS correlacionado) para portabilidade.
        StringBuilder jpql = new StringBuilder()
                .append("SELECT la.dataHora FROM LogAcesso la ")
                .append("WHERE la.resultado = :autorizado ")
                .append("AND la.tipoMovimento = :entrada ")
                .append("AND la.dataHora >= :de AND la.dataHora < :ate ")
                .append("AND (la.local IS NULL OR la.local.id IN ( ")
                .append("     SELECT es.id FROM Evento e JOIN e.espacoEventos es ")
                .append("     WHERE e.id = :eventoId)) ");
        if (locaisPermitidos != null) {
            jpql.append("AND la.local.id IN (:locaisPermitidos) ");
        }

        var query = getEntityManager().createQuery(jpql.toString(), LocalDateTime.class)
                .setParameter("autorizado", ResultadoAcesso.AUTORIZADO)
                .setParameter("entrada", TipoMovimento.ENTRADA)
                .setParameter("de", de)
                .setParameter("ate", ate)
                .setParameter("eventoId", eventoId);
        if (locaisPermitidos != null) {
            query.setParameter("locaisPermitidos", locaisPermitidos);
        }
        return query.getResultList();
    }
}
