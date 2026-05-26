package ka.mdo.tenant;

import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;

/**
 * Lê o claim "empresaId" do JWT (se houver) e popula o {@link TenantContext}
 * para que consultas/escritas possam respeitar o isolamento por tenant.
 *
 * Comportamento:
 * <ul>
 *   <li>Sem JWT (endpoints públicos): não ativa o filtro, segue o fluxo.</li>
 *   <li>JWT válido sem {@code empresaId} e SEM o perfil {@code SUPER_ADMIN}:
 *       aborta com 401 "Token sem empresaId" — evita que um token autenticado
 *       porém malformado vaze dados entre tenants (o filtro Hibernate nunca
 *       seria ativado).</li>
 *   <li>JWT com perfil {@code SUPER_ADMIN} sem {@code empresaId}: permite
 *       operação cross-tenant (filtro não é ativado).</li>
 *   <li>JWT com {@code empresaId}: popula o contexto e ativa o filtro.</li>
 * </ul>
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 100)
public class TenantRequestFilter implements ContainerRequestFilter {

    private static final String PERFIL_SUPER_ADMIN = "SUPER_ADMIN";

    @Inject
    JsonWebToken jwt;

    @Inject
    TenantContext tenantContext;

    @Inject
    TenantFilterActivator filterActivator;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            if (jwt == null || jwt.getRawToken() == null) {
                return;
            }
            Long empresaId = JwtClaims.empresaIdOrNull(jwt);
            Set<String> groups = jwt.getGroups();
            boolean isSuperAdmin = groups != null && groups.contains(PERFIL_SUPER_ADMIN);

            if (isSuperAdmin) {
                // SUPER_ADMIN pode operar cross-tenant; filtro Hibernate não é ativado
                // mesmo que o token traga empresaId (ex: conta de bootstrap atrelada
                // à empresa-sistema id=1).
                return;
            }

            if (empresaId == null) {
                requestContext.abortWith(
                        Response.status(Response.Status.UNAUTHORIZED)
                                .type(MediaType.TEXT_PLAIN)
                                .entity("Token sem empresaId")
                                .build());
                return;
            }
            tenantContext.setEmpresaId(empresaId);
            filterActivator.ativarFiltro();
        } catch (Exception e) {
            Log.warnf("Não foi possível processar o JWT no TenantRequestFilter: %s", e.getMessage());
        }
    }
}
