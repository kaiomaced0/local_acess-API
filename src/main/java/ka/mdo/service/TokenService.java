package ka.mdo.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Gera tokens opacos cripto-seguros para credenciais (Ingresso).
 *
 * <p>Estratégia: 32 bytes de {@link SecureRandom} codificados em base64url
 * sem padding, resultando em strings de 43 caracteres — confortavelmente dentro
 * do limite VARCHAR(64) do schema e com entropia suficiente (256 bits) para
 * impedir adivinhação por força bruta ou colisão acidental.
 */
@ApplicationScoped
public class TokenService {

    private static final int TOKEN_BYTES = 32;
    private final SecureRandom random = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * Gera um novo token opaco. Cada chamada produz um valor independente.
     */
    public String gerarToken() {
        byte[] buffer = new byte[TOKEN_BYTES];
        random.nextBytes(buffer);
        return encoder.encodeToString(buffer);
    }
}
