package ka.mdo.tenant;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;

/**
 * Ativa o filtro Hibernate "tenantFilter" na sessão corrente, usando o
 * empresaId presente no {@link TenantContext}. Deve ser invocado no início
 * de cada requisição autenticada (ver {@link TenantRequestFilter}).
 */
@ApplicationScoped
public class TenantFilterActivator {

    @Inject
    EntityManager entityManager;

    @Inject
    TenantContext tenantContext;

    /**
     * Ativa o filtro com o empresaId atual do contexto. Se não houver tenant,
     * o filtro não é ativado (útil para endpoints públicos).
     */
    public void ativarFiltro() {
        if (!tenantContext.temTenant()) {
            return;
        }
        try {
            Session session = entityManager.unwrap(Session.class);
            Filter filter = session.enableFilter("tenantFilter");
            filter.setParameter("empresaId", tenantContext.getEmpresaId());
        } catch (Exception e) {
            Log.errorf("Falha ao ativar tenantFilter: %s", e.getMessage());
        }
    }

    /**
     * Ativa o filtro explicitamente com um empresaId (usado em pontos onde
     * o contexto não está populado, ex: fluxos internos).
     */
    public void ativarFiltroCom(Long empresaId) {
        if (empresaId == null) {
            return;
        }
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter("tenantFilter");
        filter.setParameter("empresaId", empresaId);
    }
}
