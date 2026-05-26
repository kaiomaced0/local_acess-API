package ka.mdo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request para criação/edição de {@link ka.mdo.model.Aparelho} (atividade 014).
 *
 * <p>A empresa do aparelho vem sempre do JWT — nunca do request. O serviço
 * valida que {@code eventoId} e {@code localEspecificoId} (quando informados)
 * pertencem ao mesmo tenant; caso contrário responde 403.
 *
 * @param descricao         identificação livre (ex.: "Totem entrada principal").
 * @param eventoId          opcional. {@code null} = aparelho genérico, pode
 *                          operar em qualquer evento da empresa.
 * @param localEspecificoId opcional. {@code null} = aparelho de entrada geral
 *                          do evento (não restringe por espaço interno).
 */
public record AparelhoDTO(
        @NotBlank @Size(max = 255) String descricao,
        Long eventoId,
        Long localEspecificoId
) {
}
