package ka.mdo.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.EmpresaDTO;
import ka.mdo.service.EmpresaService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/empresas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("SUPER_ADMIN")
@Tag(name = "Empresas", description = "Gestão de tenants (apenas SUPER_ADMIN)")
public class EmpresaResource {

    @Inject
    EmpresaService service;

    @POST
    @RolesAllowed("SUPER_ADMIN")
    @Operation(summary = "Cria uma nova empresa (tenant)")
    @APIResponse(responseCode = "201", description = "Empresa criada")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    public Response insert(@Valid EmpresaDTO dto) {
        return service.insert(dto);
    }
}
