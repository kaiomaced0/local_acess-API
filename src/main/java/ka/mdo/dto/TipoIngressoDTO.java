package ka.mdo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de criação/edição de {@link ka.mdo.model.TipoIngresso} (atividade 015).
 *
 * <p>Apenas o {@code nome} é editável pelo cliente — a empresa vem do JWT,
 * o N:N com {@code EspacoEvento} é gerenciado em outro fluxo (atividade 030)
 * e o {@code ativo} é controlado pelo soft-delete do DELETE.
 */
public record TipoIngressoDTO(
        @NotBlank @Size(max = 100) String nome
) {
}
