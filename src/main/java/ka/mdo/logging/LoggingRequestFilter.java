package ka.mdo.logging;

import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logmanager.MDC;

import java.util.UUID;

/**
 * Popula o MDC (Mapped Diagnostic Context) com informações de correlação
 * para permitir que cada linha de log de uma mesma requisição seja
 * facilmente agrupada em produção:
 *
 * <ul>
 *   <li>{@code requestId} — header {@code X-Request-Id} quando presente,
 *       ou UUID gerado aqui. Também devolvido no response.</li>
 *   <li>{@code empresaId} — claim do JWT (quando autenticado).</li>
 *   <li>{@code usuarioId} — {@code sub} do JWT (quando autenticado).</li>
 * </ul>
 *
 * Roda com prioridade menor que {@link jakarta.ws.rs.Priorities#AUTHENTICATION}
 * para que o {@code requestId} já esteja no MDC antes de qualquer outro filtro
 * (ex: {@code TenantRequestFilter}) logar algo.
 *
 * <p>O MDC é limpo no response para evitar vazamento entre requisições
 * (pools de thread reaproveitam threads).
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 100)
public class LoggingRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_EMPRESA_ID = "empresaId";
    public static final String MDC_USUARIO_ID = "usuarioId";

    @Inject
    JsonWebToken jwt;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            String requestId = requestContext.getHeaderString(HEADER_REQUEST_ID);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_REQUEST_ID, requestId);

            // JWT pode estar vazio: endpoints públicos ou filtro rodando antes da auth.
            if (jwt != null && jwt.getRawToken() != null && !jwt.getClaimNames().isEmpty()) {
                Object empresaId = jwt.getClaim("empresaId");
                if (empresaId != null) {
                    MDC.put(MDC_EMPRESA_ID, String.valueOf(empresaId));
                }
                String sub = jwt.getSubject();
                if (sub != null && !sub.isBlank()) {
                    MDC.put(MDC_USUARIO_ID, sub);
                }
            }
        } catch (Exception e) {
            // Falha aqui nunca pode impedir a requisição.
            Log.debugf("LoggingRequestFilter não conseguiu popular MDC: %s", e.getMessage());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        try {
            Object requestId = MDC.get(MDC_REQUEST_ID);
            if (requestId != null && !responseContext.getHeaders().containsKey(HEADER_REQUEST_ID)) {
                responseContext.getHeaders().add(HEADER_REQUEST_ID, requestId);
            }
        } finally {
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_EMPRESA_ID);
            MDC.remove(MDC_USUARIO_ID);
        }
    }
}
