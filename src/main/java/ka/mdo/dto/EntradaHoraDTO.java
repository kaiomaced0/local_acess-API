package ka.mdo.dto;

import java.time.LocalDateTime;

/**
 * Ponto da série temporal de entradas agregadas por hora (atividade 041).
 *
 * <p>{@code hora} é o início do bucket ({@code minuto=0, segundo=0, nano=0}).
 * {@code entradas} é a quantidade de {@link ka.mdo.model.LogAcesso} com
 * {@code resultado=AUTORIZADO} e {@code tipoMovimento=ENTRADA} cuja
 * {@code dataHora} caiu naquela hora.
 */
public record EntradaHoraDTO(
        LocalDateTime hora,
        long entradas
) {
}
