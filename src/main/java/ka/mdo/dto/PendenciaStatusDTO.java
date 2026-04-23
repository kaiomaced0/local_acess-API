package ka.mdo.dto;

import ka.mdo.model.StatusPendencia;

/**
 * Contagem de pendências agrupadas por {@link StatusPendencia} (atividade 041).
 */
public record PendenciaStatusDTO(
        StatusPendencia status,
        long total
) {
}
