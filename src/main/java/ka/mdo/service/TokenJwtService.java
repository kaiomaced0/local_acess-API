package ka.mdo.service;

import ka.mdo.model.Usuario;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import io.smallrye.jwt.build.Jwt;
import java.util.stream.Collectors;

@ApplicationScoped
public class TokenJwtService {

    private static final Duration EXPIRATION_TIME = Duration.ofDays(200);

    public String generateJwt(Usuario usuario) {

        try {
            Instant now = Instant.now();

            Instant expiryDate = now.plus(EXPIRATION_TIME);

            Set<String> roles = usuario.getPerfis()
                    .stream().map(p -> p.getLabel())
                    .collect(Collectors.toSet());

            Log.info("Requisição TokenJwt.generateJwt()");

            return Jwt.issuer("jwt-kamcdo")
                    .subject(usuario.getId().toString())
                    .groups(roles)
                    .expiresAt(expiryDate)
                    .sign();

        } catch (Exception e) {
            Log.error("Erro ao rodar Requisição TokenJwt.generateJwt()");
            return null;
        }

    }

}
