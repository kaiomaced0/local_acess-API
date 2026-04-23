package ka.mdo.dto;

import ka.mdo.model.Empresa;
import ka.mdo.model.StatusEmpresa;

public record EmpresaResponseDTO(
        Long id,
        String nome,
        String cnpj,
        StatusEmpresa status
) {
    public EmpresaResponseDTO(Empresa e) {
        this(e.getId(), e.getNome(), e.getCnpj(), e.getStatus());
    }
}
