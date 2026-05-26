package ka.mdo.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.EmpresaDTO;
import ka.mdo.dto.EmpresaResponseDTO;
import ka.mdo.dto.StatusEmpresaDTO;
import ka.mdo.service.EmpresaService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/empresas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("SUPER_ADMIN")
@Tag(name = "Empresas", description = "Gestão de tenants (apenas SUPER_ADMIN)")
public class EmpresaResource {

    @Inject
    EmpresaService service;

    @POST
    @Operation(summary = "Cria uma nova empresa (tenant)")
    @APIResponse(responseCode = "201", description = "Empresa criada")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "409", description = "CNPJ já cadastrado")
    public Response insert(@Valid EmpresaDTO dto) {
        return service.insert(dto);
    }

    @GET
    @Operation(summary = "Lista empresas (paginado)")
    @APIResponse(responseCode = "200", description = "Página de empresas")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    public List<EmpresaResponseDTO> listar(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("incluirInativas") @DefaultValue("false") boolean incluirInativas) {
        return service.listar(incluirInativas, page, size);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca empresa por id")
    @APIResponse(responseCode = "200", description = "Empresa encontrada")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "404", description = "Empresa não encontrada")
    public Response getById(
            @PathParam("id") Long id,
            @QueryParam("incluirInativas") @DefaultValue("false") boolean incluirInativas) {
        EmpresaResponseDTO empresa = service.buscarPorId(id, incluirInativas);
        if (empresa == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(empresa).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza dados cadastrais da empresa (nome/cnpj)")
    @APIResponse(responseCode = "200", description = "Empresa atualizada")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "404", description = "Empresa não encontrada")
    @APIResponse(responseCode = "409", description = "CNPJ já cadastrado em outra empresa")
    public Response atualizar(@PathParam("id") Long id, @Valid EmpresaDTO dto) {
        return service.atualizar(id, dto);
    }

    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Transiciona o status da empresa (ATIVA/SUSPENSA/ENCERRADA)")
    @APIResponse(responseCode = "200", description = "Status atualizado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "404", description = "Empresa não encontrada")
    @APIResponse(responseCode = "409", description = "Transição de status inválida")
    public Response atualizarStatus(@PathParam("id") Long id, @Valid StatusEmpresaDTO dto) {
        return service.atualizarStatus(id, dto);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Soft-delete da empresa (marca ativo=false)")
    @APIResponse(responseCode = "204", description = "Empresa marcada como inativa")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "404", description = "Empresa não encontrada")
    public Response delete(@PathParam("id") Long id) {
        return service.softDelete(id);
    }
}
