package ka.mdo.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ka.mdo.model.TipoDocumento;

/**
 * Request para criar/atualizar dados pessoais do usuário logado.
 *
 * <p>Para {@code tipoDocumento == CPF}, o service remove máscara e valida
 * dígitos verificadores — um CPF inválido resulta em {@code 400 Bad Request}.
 * Para demais tipos aceitamos string livre (não validamos formato por ora).
 */
public record DadosPessoaisDTO(
        @NotBlank
        @Size(min = 3, max = 150)
        String nomeCompleto,
        @NotNull
        TipoDocumento tipoDocumento,
        @NotBlank
        @Size(min = 3, max = 40)
        String documento,
        LocalDate dataNascimento
) {
}
