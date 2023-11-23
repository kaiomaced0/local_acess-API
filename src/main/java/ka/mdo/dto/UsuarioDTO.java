package ka.mdo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

public record UsuarioDTO(
                @NotBlank @NotNull @Size(min = 11) @CPF(message = "cpf inválido") String cpf,
                @NotBlank @NotNull @Size(min = 3, max = 70) String nome,
                @NotBlank @NotNull  @Size(min = 8, max = 200) @Email(message = "email inválido") String email,
                @NotBlank @NotNull @Size(min = 3, max = 800) String senha) {

}