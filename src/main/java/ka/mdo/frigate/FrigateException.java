package ka.mdo.frigate;

/**
 * Erro na comunicação com o Frigate (timeout, 5xx, resposta malformada).
 * O {@code FacialValidationService} trata como "indisponível" e aplica a
 * política de fallback configurada em {@code frigate.fallback}.
 */
public class FrigateException extends RuntimeException {

    public FrigateException(String message) {
        super(message);
    }

    public FrigateException(String message, Throwable cause) {
        super(message, cause);
    }
}
