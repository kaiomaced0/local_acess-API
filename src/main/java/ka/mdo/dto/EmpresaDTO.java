package ka.mdo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmpresaDTO(
        @NotNull @NotBlank @Size(max = 255) String nome,
        @Size(max = 20) String cnpj
) {
}
