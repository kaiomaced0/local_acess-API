package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.GestorLocal;

import java.util.List;
import java.util.Optional;

/**
 * Repositório do vínculo {@link GestorLocal} (atividade 041). O filtro
 * {@code tenantFilter} do Hibernate isola por empresa — as queries abaixo não
 * dependem de {@code empresaId} vindo do cliente.
 */
@ApplicationScoped
public class GestorLocalRepository implements PanacheRepository<GestorLocal> {

    /**
     * Lista os ids de {@link ka.mdo.model.EspacoEvento} gerenciados pelo
     * usuário. Usado para restringir visibilidade de gestores de local em
     * pendências, logs e métricas.
     *
     * <p>Retorna lista vazia (não {@code null}) quando o usuário não
     * gerencia nenhum local.
     */
    public List<Long> findLocaisDoGestor(Long usuarioId) {
        if (usuarioId == null) {
            return List.of();
        }
        return getEntityManager().createQuery(
                        "SELECT gl.local.id FROM GestorLocal gl "
                                + "WHERE gl.gestor.id = :usuarioId", Long.class)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
    }

    /**
     * Busca o vínculo específico (usado para idempotência no POST / lookup no DELETE).
     */
    public Optional<GestorLocal> findByUsuarioELocal(Long usuarioId, Long localId) {
        if (usuarioId == null || localId == null) {
            return Optional.empty();
        }
        return find("gestor.id = ?1 AND local.id = ?2", usuarioId, localId)
                .firstResultOptional();
    }

    /**
     * Lista ids dos usuários {@code GESTOR_LOCAL} vinculados a um
     * {@link ka.mdo.model.EspacoEvento} específico. Usado pelo
     * {@code PendenciaService} para notificar apenas quem deve ver a
     * pendência (débito 031 fechado).
     */
    public List<Long> findGestoresDoLocal(Long localId) {
        if (localId == null) {
            return List.of();
        }
        return getEntityManager().createQuery(
                        "SELECT gl.gestor.id FROM GestorLocal gl "
                                + "WHERE gl.local.id = :localId", Long.class)
                .setParameter("localId", localId)
                .getResultList();
    }
}
