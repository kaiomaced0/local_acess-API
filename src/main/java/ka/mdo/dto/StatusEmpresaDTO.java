package ka.mdo.dto;

import jakarta.validation.constraints.NotNull;
import ka.mdo.model.StatusEmpresa;

/**
 * Request DTO para transição explícita de {@link StatusEmpresa}
 * (atividade 008). Usado por {@code PATCH /empresas/{id}/status}.
 */
public record StatusEmpresaDTO(
        @NotNull StatusEmpresa status
) {
}
