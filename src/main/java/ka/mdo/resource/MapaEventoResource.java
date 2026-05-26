package ka.mdo.resource;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import ka.mdo.dto.MapaEventoDTO;
import ka.mdo.dto.MapaEventoResponseDTO;
import ka.mdo.service.MapaEventoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Endpoints do mapa 2D do evento (atividade 040).
 *
 * <p><b>Autorização</b>:
 * <ul>
 *   <li>Leitura ({@link #getMapa}): qualquer perfil autenticado do tenant
 *       — aparelhos e clientes precisam ler o mapa para visualização.</li>
 *   <li>Escrita ({@link #salvar}, {@link #uploadImagemFundo}): apenas
 *       {@code GESTOR_EVENTO}, {@code ADMIN_EMPRESA} ou
 *       {@code SUPER_ADMIN}.</li>
 * </ul>
 *
 * <p>Upload segue o mesmo padrão da 020: {@code application/octet-stream}
 * no body + header {@code X-Content-Type} com o MIME real. Evita depender
 * de {@code quarkus-resteasy-multipart}.
 */
@Path("/eventos/{eventoId}/mapa")
@Authenticated
@Tag(name = "Mapa do evento", description = "Mapa 2D do evento com polígonos por EspacoEvento")
public class MapaEventoResource {

    @Inject
    MapaEventoService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SUPER_ADMIN", "ADMIN_EMPRESA", "GESTOR_EVENTO", "GESTOR_LOCAL", "OPERADOR_APARELHO", "CLIENTE"})
    @Operation(summary = "Retorna o mapa do evento com as formas cadastradas")
    @APIResponse(responseCode = "200", description = "Mapa encontrado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Evento pertence a outra empresa")
    @APIResponse(responseCode = "404", description = "Evento sem mapa cadastrado")
    public MapaEventoResponseDTO getMapa(@PathParam("eventoId") Long eventoId) {
        return service.buscarPorEvento(eventoId);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SUPER_ADMIN", "ADMIN_EMPRESA", "GESTOR_EVENTO"})
    @Operation(summary = "Cria ou substitui o mapa do evento",
            description = "Upsert — se já houver mapa, as formas antigas são removidas " +
                    "(orphanRemoval) e as novas persistidas.")
    @APIResponse(responseCode = "200", description = "Mapa salvo")
    @APIResponse(responseCode = "400", description = "Payload inválido (cor, geometria ou espaço fora do evento)")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Evento ou espaço pertence a outra empresa")
    @APIResponse(responseCode = "404", description = "Evento ou EspacoEvento inexistente")
    public MapaEventoResponseDTO salvar(@PathParam("eventoId") Long eventoId,
                                        @Valid MapaEventoDTO dto) {
        return service.salvar(eventoId, dto);
    }

    @POST
    @Path("/imagem-fundo")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SUPER_ADMIN", "ADMIN_EMPRESA", "GESTOR_EVENTO"})
    @Operation(summary = "Upload da imagem de fundo do mapa",
            description = "Envie os bytes crus com Content-Type: application/octet-stream " +
                    "e o MIME real (ex: image/png) no header X-Content-Type.")
    @APIResponse(responseCode = "200", description = "Imagem enviada")
    @APIResponse(responseCode = "400", description = "Arquivo inválido ou MIME não permitido")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Evento pertence a outra empresa")
    @APIResponse(responseCode = "404", description = "Evento sem mapa — faça PUT /mapa antes")
    public MapaEventoResponseDTO uploadImagemFundo(@PathParam("eventoId") Long eventoId,
                                                   @HeaderParam("X-Content-Type") String contentType,
                                                   byte[] bytes) {
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("Header X-Content-Type obrigatório (ex: image/png)");
        }
        return service.uploadImagemFundo(eventoId, bytes, contentType);
    }
}
