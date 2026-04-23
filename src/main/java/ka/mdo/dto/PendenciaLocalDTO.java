package ka.mdo.dto;

/**
 * Contagem de pendências {@link ka.mdo.model.StatusPendencia#ABERTA}
 * agrupadas por {@link ka.mdo.model.EspacoEvento} (atividade 041).
 *
 * <p>{@code localId}/{@code localNome} podem ser {@code null} quando a
 * pendência foi aberta sem local específico (aparelho de entrada geral).
 */
public record PendenciaLocalDTO(
        Long localId,
        String localNome,
        long abertas
) {
}
