package ka.mdo.resource;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.IngressoDTO;
import ka.mdo.dto.UsuarioDTO;
import ka.mdo.dto.UsuarioResponseDTO;
import ka.mdo.model.Usuario;
import ka.mdo.service.IngressoService;
import ka.mdo.service.UsuarioService;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/usuarios")
@RolesAllowed({"SUPER_ADMIN", "ADMIN_EMPRESA"})
@Tag(name = "Usuários", description = "Gestão de usuários da empresa")
public class UsuarioResource {
    @Inject
    private UsuarioService service;

    @Inject
    IngressoService ingressoService;


    @GET
    @Operation(summary = "Lista usuários da empresa")
    @APIResponse(responseCode = "200", description = "Lista de usuários")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    public List<UsuarioResponseDTO> getAll(){
        return service.findAll();
    }
    @GET
    @Path("/{id}")
    @Operation(summary = "Busca usuário por id")
    @APIResponse(responseCode = "200", description = "Usuário encontrado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "404", description = "Usuário não encontrado")
    public UsuarioResponseDTO getById(@PathParam("id") Long id) {
        return service.findById(id);
    }

    @POST
    @Operation(summary = "Cria um novo usuário")
    @APIResponse(responseCode = "201", description = "Usuário criado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    public Response create(UsuarioDTO entity) {
        return service.insert(entity);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza um usuário existente")
    @APIResponse(responseCode = "200", description = "Usuário atualizado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "404", description = "Usuário não encontrado")
    public Usuario update(@PathParam("id") Long id, Usuario entity) {
        return service.update(entity);
    }

    @POST
    @Path("/{idUsuario}/ingressos")
    @RolesAllowed({"SUPER_ADMIN", "ADMIN_EMPRESA", "GESTOR_EVENTO"})
    @Operation(summary = "Emite uma credencial para o usuário (atividade 013)",
            description = "Cria um Ingresso vinculado ao usuário do tenant, gera o token opaco (base do QR) e devolve o IngressoResponseDTO. Aceita o campo opcional escopoGlobal (atividade 033) com o gate de papel já aplicado no service.")
    @APIResponse(responseCode = "201", description = "Credencial emitida")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Usuário/TipoIngresso de outro tenant, ou perfil sem direito de emitir credencial global")
    public Response emitirIngresso(@PathParam("idUsuario") Long idUsuario, IngressoDTO dto) {
        return ingressoService.adicionarIngresso(idUsuario, dto);
    }

    @PATCH
    @Path("/delete/{id}")
    @Operation(summary = "Soft-delete de um usuário")
    @APIResponse(responseCode = "204", description = "Usuário marcado como excluído")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "404", description = "Usuário não encontrado")
    public void delete(@PathParam("id") Long id) {
        service.deleteById(id);
    }
}
