package ka.mdo.resource;

import ka.mdo.dto.AuthUsuarioDTO;
import ka.mdo.dto.UsuarioResponseDTO;
import ka.mdo.model.Usuario;
import ka.mdo.service.TokenJwtService;
import ka.mdo.service.UsuarioService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Autenticação", description = "Login e emissão de token JWT")
public class AuthResource {

    @Inject
    UsuarioService usuarioService;

    @Inject
    TokenJwtService tokenService;

    @Inject
    JsonWebToken jsonWebToken;

    @POST
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Autentica um usuário e retorna o token JWT")
    @APIResponse(responseCode = "200", description = "Login bem-sucedido; token no header Authorization")
    @APIResponse(responseCode = "204", description = "Usuário não encontrado ou credenciais inválidas")
    public Response login(AuthUsuarioDTO authDTO) {

        Usuario usuario = usuarioService.byLoginAndSenha(authDTO);

        if (usuario == null) {
            return Response.status(Status.NO_CONTENT)
                    .entity("Usuario não encontrado").build();
        }
        return Response.ok()
                .header("Authorization", tokenService.generateJwt(usuario))
                .header("usuarioLogado", new UsuarioResponseDTO(usuario))
                .build();

    }

}
