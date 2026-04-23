package ka.mdo.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ka.mdo.model.GestorLocal;
import ka.mdo.service.GestorLocalService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Endpoints para gerenciar o vínculo {@link GestorLocal} (atividade 041).
 *
 * <p>Restrito a {@code ADMIN_EMPRESA} e {@code SUPER_ADMIN}. Fecha os débitos
 * 030 e 031: com o vínculo persistido, {@code GESTOR_LOCAL} passa a ter
 * visibilidade reduzida em logs, pendências e métricas.
 */
@Path("/api/v1/gestores")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN_EMPRESA", "SUPER_ADMIN"})
@Tag(name = "GestorLocal", description = "Vínculo gestor↔local para visibilidade do GESTOR_LOCAL")
public class GestorLocalResource {

    @Inject
    GestorLocalService gestorLocalService;

    @POST
    @Path("/{usuarioId}/locais/{localId}")
    @Operation(summary = "Vincula um gestor a um local (idempotente)",
            description = "Usuário alvo precisa ter perfil GESTOR_LOCAL. "
                    + "Vínculo duplicado é no-op (devolve 200 com o vínculo existente).")
    @APIResponse(responseCode = "200", description = "Vínculo criado ou já existente")
    @APIResponse(responseCode = "400", description = "Usuário sem perfil GESTOR_LOCAL")
    @APIResponse(responseCode = "403", description = "Usuário ou local de outra empresa")
    @APIResponse(responseCode = "404", description = "Usuário ou local inexistente")
    public Response vincular(@PathParam("usuarioId") Long usuarioId,
                             @PathParam("localId") Long localId) {
        try {
            GestorLocal gl = gestorLocalService.vincular(usuarioId, localId);
            return Response.ok().entity(new VinculoResponse(
                    gl.getId(),
                    gl.getGestor().getId(),
                    gl.getLocal().getId(),
                    gl.getEmpresa().getId()
            )).build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @DELETE
    @Path("/{usuarioId}/locais/{localId}")
    @Operation(summary = "Remove o vínculo gestor↔local",
            description = "404 se o vínculo não existir.")
    @APIResponse(responseCode = "204", description = "Vínculo removido")
    @APIResponse(responseCode = "403", description = "Vínculo de outra empresa")
    @APIResponse(responseCode = "404", description = "Vínculo inexistente")
    public Response desvincular(@PathParam("usuarioId") Long usuarioId,
                                @PathParam("localId") Long localId) {
        gestorLocalService.desvincular(usuarioId, localId);
        return Response.noContent().build();
    }

    /** Payload enxuto — entidade nunca é serializada. */
    public record VinculoResponse(Long id, Long usuarioId, Long localId, Long empresaId) {}
}
