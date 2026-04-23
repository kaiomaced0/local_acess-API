package ka.mdo.service;

import ka.mdo.model.Perfil;
import ka.mdo.model.Usuario;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
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

            Set<Perfil> perfis = usuario.getPerfis() != null ? usuario.getPerfis() : Collections.emptySet();

            // `groups` é o que o quarkus-smallrye-jwt usa para casar com @RolesAllowed.
            // Emitimos o nome do enum (ex: "SUPER_ADMIN") para permitir
            // @RolesAllowed("SUPER_ADMIN") diretamente.
            Set<String> groups = perfis.stream()
                    .map(Perfil::name)
                    .collect(Collectors.toSet());

            // `perfil` (label amigável) é mantido para compatibilidade com clientes
            // que já consumiam essa claim.
            Set<String> labels = perfis.stream()
                    .map(Perfil::getLabel)
                    .collect(Collectors.toSet());

            Long empresaId = usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null;

            Log.info("Requisição TokenJwt.generateJwt()");

            return Jwt.issuer("jwt-kamcdo")
                    .subject(usuario.getId().toString())
                    .groups(groups)
                    .claim("empresaId", empresaId)
                    .claim("perfil", labels)
                    .expiresAt(expiryDate)
                    .sign();

        } catch (Exception e) {
            Log.error("Erro ao rodar Requisição TokenJwt.generateJwt()");
            return null;
        }

    }

}
