package ka.mdo.testsupport;

import io.smallrye.jwt.build.Jwt;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Geração de tokens JWT reais para os testes de integração (atividade 051).
 *
 * <p>Assina com a mesma chave privada da aplicação ({@code privateKey.pem},
 * via {@code smallrye.jwt.sign.key.location}) e usa o mesmo issuer
 * ({@code jwt-kamcdo}), de modo que o token passa pela verificação real do
 * {@code quarkus-smallrye-jwt} — exercitando {@code @RolesAllowed} e o
 * {@code TenantRequestFilter} de verdade. Espelha o {@code TokenJwtService}.
 *
 * <p>Mintar o token diretamente (em vez de criar um {@code Usuario} e logar)
 * evita depender de linhas reais para papéis e contorna a validação de CPF na
 * persistência de {@code Usuario}.
 */
public final class TestJwt {

    private TestJwt() {
    }

    /** Valor pronto para o header {@code Authorization} ({@code "Bearer <jwt>"}). */
    public static String bearer(Long empresaId, Long subject, String... groups) {
        Set<String> grupos = new HashSet<>(Arrays.asList(groups));
        var builder = Jwt.issuer("jwt-kamcdo")
                .subject(subject == null ? "0" : String.valueOf(subject))
                .groups(grupos)
                .expiresIn(Duration.ofHours(2));
        if (empresaId != null) {
            builder = builder.claim("empresaId", empresaId);
        }
        return "Bearer " + builder.sign();
    }
}
