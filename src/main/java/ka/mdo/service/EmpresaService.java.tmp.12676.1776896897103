package ka.mdo.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.EmpresaDTO;
import ka.mdo.dto.EmpresaResponseDTO;
import ka.mdo.model.Empresa;
import ka.mdo.model.StatusEmpresa;
import ka.mdo.repository.EmpresaRepository;

@ApplicationScoped
public class EmpresaService {

    @Inject
    EmpresaRepository repository;

    @Transactional
    public Response insert(EmpresaDTO dto) {
        try {
            if (dto.cnpj() != null && repository.findByCnpj(dto.cnpj()) != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Já existe empresa com esse CNPJ").build();
            }
            Empresa empresa = new Empresa();
            empresa.setNome(dto.nome());
            empresa.setCnpj(dto.cnpj());
            empresa.setStatus(StatusEmpresa.ATIVA);
            repository.persist(empresa);
            Log.info("Empresa cadastrada: " + empresa.getId());
            return Response.ok(new EmpresaResponseDTO(empresa)).build();
        } catch (Exception e) {
            Log.error("Erro ao cadastrar empresa: " + e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
