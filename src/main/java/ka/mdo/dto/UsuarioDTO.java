package ka.mdo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioDTO(
                @NotBlank @Size(min = 11, max = 14) String cpf,
                @NotBlank @Size(min = 3, max = 70) String nome,
                @NotBlank @Size(min = 8, max = 200) @Email String email,
                @NotBlank @Size(min = 3, max = 800) String senha) {

}