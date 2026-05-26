package ka.mdo.tenant;

import jakarta.json.JsonNumber;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Leitura tolerante de claims do JWT.
 *
 * <p>Em algumas versões do SmallRye JWT, claims numéricos são retornados como
 * {@link JsonNumber} (wrapper do JSON-P), não como {@link Long}. Um cast direto
 * {@code Long x = jwt.getClaim(...)} lança {@code ClassCastException}. Esta
 * classe normaliza essa leitura para o tipo Java esperado e nunca explode em
 * runtime — basta haver representação numérica válida.
 */
public final class JwtClaims {

    private JwtClaims() {
    }

    /**
     * Lê o claim {@code empresaId} retornando {@code null} se ausente.
     * Aceita {@link Number}, {@link JsonNumber} e {@link String} numéricos.
     */
    public static Long empresaIdOrNull(JsonWebToken jwt) {
        if (jwt == null) {
            return null;
        }
        Object raw = jwt.getClaim("empresaId");
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw instanceof JsonNumber jn) {
            return jn.longValueExact();
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return Long.valueOf(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
