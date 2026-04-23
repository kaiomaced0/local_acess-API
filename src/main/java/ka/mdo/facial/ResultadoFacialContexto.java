package ka.mdo.facial;

/**
 * Encapsula o resultado completo da validação facial para o caller
 * ({@code AcessoService}). Inclui a chave do objeto da captura (sempre
 * persistida, independente do resultado) para alimentar o {@code LogAcesso}.
 *
 * @param resultado           decisão do passo facial.
 * @param capturaObjectKey    chave da foto capturada armazenada em
 *                            {@code capturas-acesso/}; {@code null} só em
 *                            falhas extremas (upload falhou).
 * @param score               similaridade retornada pelo Frigate (0..1); 0
 *                            quando {@code CADASTRADO} ou sem match.
 * @param motivoPublico       código estável para devolver ao aparelho quando
 *                            o resultado não for {@code AUTORIZADO}/{@code CADASTRADO}.
 *                            Ex: {@code ROSTO_DIVERGENTE}, {@code FRIGATE_INDISPONIVEL}.
 */
public record ResultadoFacialContexto(
        ResultadoFacial resultado,
        String capturaObjectKey,
        double score,
        String motivoPublico
) {
}
