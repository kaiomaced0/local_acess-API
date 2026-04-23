package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import ka.mdo.model.Ingresso;
import ka.mdo.tenant.TenantContext;
import org.hibernate.Session;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class IngressoRepository implements PanacheRepository<Ingresso> {

    @Inject
    EntityManager entityManager;

    @Inject
    TenantContext tenantContext;

    public List<Ingresso> findByNome(String nome) {
        if (nome == null)
            return null;
        return find("UPPER(nome) LIKE ?1 ", "%" + nome.toUpperCase() + "%").list();
    }

    /**
     * Busca credencial pelo token opaco. Consumido pelo endpoint de validação de
     * acesso (atividade 011). Retorna {@link Optional#empty()} quando não há
     * credencial com o token informado.
     */
    public Optional<Ingresso> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return find("token = ?1", token).firstResultOptional();
    }

    /**
     * Atividade 033: busca credencial pelo token SEM aplicar o
     * {@code tenantFilter}. Usado exclusivamente para suportar credenciais
     * globais {@code SUPER} (cross-tenant): quando o aparelho pertence a
     * uma empresa diferente da empresa emissora da credencial, o
     * {@link #findByToken(String)} filtrado retornaria vazio. Esta variante
     * desativa o filtro temporariamente, consulta e reativa — o chamador é
     * responsável por validar o {@code escopoGlobal=SUPER} antes de
     * autorizar qualquer operação cross-tenant. Não use para credenciais
     * comuns.
     */
    public Optional<Ingresso> findByTokenCrossTenant(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Session session = entityManager.unwrap(Session.class);
        boolean estavaAtivo = session.getEnabledFilter("tenantFilter") != null;
        if (estavaAtivo) {
            session.disableFilter("tenantFilter");
        }
        try {
            return find("token = ?1", token).firstResultOptional();
        } finally {
            // Reativa o filtro com o empresaId do contexto — fonte única
            // de verdade durante o request (ver TenantFilterActivator).
            if (estavaAtivo && tenantContext.temTenant()) {
                session.enableFilter("tenantFilter")
                        .setParameter("empresaId", tenantContext.getEmpresaId());
            }
        }
    }
}
