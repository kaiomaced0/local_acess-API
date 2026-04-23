package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.Pendencia;
import ka.mdo.model.StatusPendencia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositório de {@link Pendencia} (atividade 031). O filtro multi-tenant do
 * Hibernate ({@code tenantFilter}) já isola por empresa — as queries não
 * dependem de {@code empresaId} vindo do cliente.
 */
@ApplicationScoped
public class PendenciaRepository implements PanacheRepository<Pendencia> {

    /**
     * Busca paginada com filtros opcionais.
     *
     * <p>Ordenação: mais recentes primeiro ({@code criadaEm DESC}) — coerente
     * com o índice composto {@code (empresa_id, status, criadaEm DESC)}.
     *
     * @param status           filtro opcional.
     * @param localId          filtro opcional.
     * @param locaisPermitidos se {@code != null}, restringe a esses locais
     *                         (visão do GESTOR_LOCAL). Coleção vazia ⇒
     *                         nenhum resultado.
     * @param pageIndex        0-based.
     * @param pageSize         tamanho da página.
     */
    public List<Pendencia> buscar(StatusPendencia status,
                                  Long localId,
                                  Collection<Long> locaisPermitidos,
                                  int pageIndex,
                                  int pageSize) {
        List<String> clauses = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (status != null) {
            clauses.add("status = :status");
            params.put("status", status);
        }
        if (localId != null) {
            clauses.add("local.id = :localId");
            params.put("localId", localId);
        }
        if (locaisPermitidos != null) {
            if (locaisPermitidos.isEmpty()) {
                return List.of();
            }
            // Inclui pendências sem local (entrada geral) para que o GESTOR_LOCAL
            // não perca visibilidade de pendências disparadas sem local específico.
            clauses.add("(local IS NULL OR local.id IN (:locaisPermitidos))");
            params.put("locaisPermitidos", locaisPermitidos);
        }

        String where = clauses.isEmpty() ? "1=1" : String.join(" AND ", clauses);

        return find(where, Sort.by("criadaEm").descending(), params)
                .page(Page.of(pageIndex, pageSize))
                .list();
    }

    /**
     * Busca pendência {@link StatusPendencia#ABERTA} para a credencial
     * informada. Usado pelo {@code PendenciaService#criar} para garantir
     * idempotência — não queremos duplicar registros nem spam de notificações
     * quando o mesmo aparelho retenta logo em seguida.
     *
     * <p>Caso específico: se houver mais de uma ABERTA (corrida entre
     * observers), devolvemos a primeira encontrada. A política anti-duplicação
     * real é implementada no service.
     */
    public Optional<Pendencia> findPendenteAberta(Long credencialId) {
        if (credencialId == null) {
            return Optional.empty();
        }
        return find("credencial.id = ?1 AND status = ?2",
                credencialId, StatusPendencia.ABERTA)
                .firstResultOptional();
    }

    /**
     * Variante útil para o {@code PendenciaService}: pendência ABERTA para
     * {@code (credencial, motivo)} — é nisso que se baseia a idempotência
     * descrita no enunciado (mesma credencial, mesmo motivo ⇒ mesma pendência).
     */
    public Optional<Pendencia> findAbertaPorMotivo(Long credencialId, String motivo) {
        if (credencialId == null || motivo == null) {
            return Optional.empty();
        }
        return find("credencial.id = ?1 AND motivo = ?2 AND status = ?3",
                credencialId, motivo, StatusPendencia.ABERTA)
                .firstResultOptional();
    }

    /**
     * Total de pendências agrupado por {@link StatusPendencia} (atividade 041).
     * O filtro Hibernate {@code tenantFilter} já isola por tenant. Apenas
     * considera pendências cuja credencial é de ingressos do evento
     * requisitado — usamos o vínculo {@code Usuario.ingressos} indiretamente
     * via {@code credencial.tipoIngresso.evento} quando existir, senão
     * consideramos todas do tenant (pendências raramente não têm evento
     * associável, mas a query é tolerante).
     *
     * <p>Como {@code Ingresso} não tem FK direta para {@code Evento} (o
     * vínculo é por {@code Aparelho.evento}), este método agrupa por status
     * tudo do tenant — o recorte por evento é feito em memória pelo service
     * quando precisar (hoje o dashboard mostra o total do tenant; é o
     * comportamento usado em 031).
     *
     * @param locaisPermitidos visão do GESTOR_LOCAL. {@code null} = sem
     *                         restrição adicional.
     */
    public List<Object[]> totalPorStatus(Collection<Long> locaisPermitidos) {
        if (locaisPermitidos != null && locaisPermitidos.isEmpty()) {
            return List.of();
        }
        StringBuilder jpql = new StringBuilder(
                "SELECT p.status, COUNT(p) FROM Pendencia p ");
        if (locaisPermitidos != null) {
            jpql.append("WHERE (p.local IS NULL OR p.local.id IN (:locaisPermitidos)) ");
        }
        jpql.append("GROUP BY p.status");
        var query = getEntityManager().createQuery(jpql.toString(), Object[].class);
        if (locaisPermitidos != null) {
            query.setParameter("locaisPermitidos", locaisPermitidos);
        }
        return query.getResultList();
    }

    /**
     * Pendências {@link StatusPendencia#ABERTA} agrupadas por
     * {@link ka.mdo.model.EspacoEvento}. Pendências sem local vêm com
     * {@code localId=null, localNome=null}. Atividade 041.
     */
    public List<Object[]> abertasPorLocal(Collection<Long> locaisPermitidos) {
        if (locaisPermitidos != null && locaisPermitidos.isEmpty()) {
            return List.of();
        }
        StringBuilder jpql = new StringBuilder()
                .append("SELECT p.local.id, p.local.nome, COUNT(p) FROM Pendencia p ")
                .append("WHERE p.status = :aberta ");
        if (locaisPermitidos != null) {
            jpql.append("AND (p.local IS NULL OR p.local.id IN (:locaisPermitidos)) ");
        }
        jpql.append("GROUP BY p.local.id, p.local.nome");
        var query = getEntityManager().createQuery(jpql.toString(), Object[].class)
                .setParameter("aberta", StatusPendencia.ABERTA);
        if (locaisPermitidos != null) {
            query.setParameter("locaisPermitidos", locaisPermitidos);
        }
        return query.getResultList();
    }
}
