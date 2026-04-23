package ka.mdo.frigate;

/**
 * Representa um match retornado pelo Frigate ao comparar um rosto capturado
 * contra a base enrolada. A decisão de {@code AUTORIZADO}/{@code DIVERGENTE}
 * é tomada no domínio (comparando {@code score} com o threshold configurado).
 *
 * @param pessoaId identificador do rosto no Frigate ({@code null} quando não
 *                 há match ou quando o Frigate ainda não retornou correspondência).
 * @param score    similaridade entre 0.0 e 1.0 (1.0 = match perfeito).
 */
public record FrigateRostoMatch(String pessoaId, double score) {
}
