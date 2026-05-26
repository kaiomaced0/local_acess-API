package ka.mdo.resource;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.UsuarioResponseDTO;
import ka.mdo.repository.UsuarioRepository;
import ka.mdo.service.UsuarioLogadoService;

@Path("/usuario-logado")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Usuário logado", description = "Dados do usuário autenticado pelo token atual")
public class UsuarioLogadoResource {
    @Inject
    UsuarioLogadoService usuarioLogadoService;

    @Inject
    UsuarioRepository repository;

    @Inject
    JsonWebToken jsonWebToken;

    @GET
    @Operation(summary = "Retorna os dados do usuário autenticado")
    @APIResponse(responseCode = "200", description = "Dados do usuário logado")
    @APIResponse(responseCode = "204", description = "Usuário não localizado a partir do token")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    public Response getUsuarioLogado() {
        try {
            return Response.ok(new UsuarioResponseDTO(repository.findByIdModificado(jsonWebToken.getSubject()))).build();
        }catch (Exception e){
            return Response.status(Response.Status.NO_CONTENT).build();
        }

    }
}
