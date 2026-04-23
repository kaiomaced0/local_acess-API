package ka.mdo.model;

/**
 * Tipo do documento de identificação informado em {@link DadosPessoais}.
 *
 * <ul>
 *     <li>{@link #CPF}: persistido sempre em formato só-dígitos (11 chars);
 *     o {@code DadosPessoaisService} remove máscara e valida dígitos
 *     verificadores antes de gravar.</li>
 *     <li>{@link #RG}: aceito como string livre (cada UF tem formato
 *     próprio). Sem validação de dígito verificador — apenas comprimento.</li>
 *     <li>{@link #PASSAPORTE}: aceito como string livre (letras + dígitos).</li>
 *     <li>{@link #OUTRO}: fallback para documentos estrangeiros/corporativos
 *     não cobertos pelos demais — sem validação de formato.</li>
 * </ul>
 */
public enum TipoDocumento {
    CPF,
    RG,
    PASSAPORTE,
    OUTRO
}
