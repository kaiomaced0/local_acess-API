package ka.mdo.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Payload do PUT /api/v1/espacos-evento/{espacoId}/autorizacoes — substitui
 * a whitelist por completo (atividade 030). Conjunto vazio é válido e
 * significa "sem restrição" (política: autoriza, ver {@code AcessoService}).
 */
public record AutorizacaoDTO(
        @NotNull Set<Long> tiposIngressoIds
) {
}
