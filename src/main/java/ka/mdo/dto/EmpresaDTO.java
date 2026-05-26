package ka.mdo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

/**
 * Request DTO de criação/atualização de {@code Empresa}.
 *
 * <p>CNPJ é opcional (algumas empresas-piloto/teste podem entrar sem CNPJ),
 * mas quando informado precisa ser válido — daí o {@code @CNPJ} (atividade
 * 008). O status nunca é trafegado por aqui: criação fixa {@code ATIVA} e
 * transições explícitas vão por {@code PATCH /empresas/{id}/status}.
 */
public record EmpresaDTO(
        @NotNull @NotBlank @Size(max = 255) String nome,
        @CNPJ(message = "cnpj inválido") @Size(max = 20) String cnpj
) {
}
