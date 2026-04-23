package ka.mdo.logging;

/**
 * Utilitário para mascarar valores sensíveis antes de cair em linhas de log.
 *
 * <p>É apoio opcional: o projeto ainda não reescreve todos os loggers
 * existentes. Quando um novo logger precisar imprimir valor que pode
 * conter segredo (token, senha, documento), passe pelo
 * {@link #mascarar(String)}.
 *
 * <p>Heurística atual: strings com mais de 20 caracteres, sem espaços
 * e compostas apenas por caracteres alfanuméricos (ou sinais comuns em
 * tokens como {@code . _ - =}) são consideradas segredos e substituídas
 * por {@code ***}. Para os demais casos o valor é devolvido como veio.
 */
public final class LogSanitizer {

    private static final int TAMANHO_MINIMO_SUSPEITA = 20;
    private static final String MASCARA = "***";

    private LogSanitizer() {
    }

    public static String mascarar(String valor) {
        if (valor == null || valor.isEmpty()) {
            return valor;
        }
        if (valor.length() <= TAMANHO_MINIMO_SUSPEITA) {
            return valor;
        }
        if (valor.indexOf(' ') >= 0) {
            return valor;
        }
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            boolean alfanumerico = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z');
            boolean permitidoEmToken = c == '.' || c == '_' || c == '-' || c == '=';
            if (!alfanumerico && !permitidoEmToken) {
                return valor;
            }
        }
        return MASCARA;
    }
}
