package ka.mdo.resource;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.EspacoEventoDTO;
import ka.mdo.dto.EventoDTO;
import ka.mdo.dto.EventoResponseDTO;
import ka.mdo.service.EventoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/v1/eventos")
@Authenticated
@Tag(name = "Eventos", description = "Gestão de eventos e espaços de evento da empresa")
public class EventoResource {

    @Inject
    private EventoService service;

    // Listagem: qualquer usuário autenticado da empresa (filtro de tenant já isola).
    @GET
    @Operation(summary = "Lista eventos da empresa autenticada")
    @APIResponse(responseCode = "200", description = "Lista de eventos")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    public List<EventoResponseDTO> getAll(){
        return service.findAll();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca evento por id")
    @APIResponse(responseCode = "200", description = "Evento encontrado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "404", description = "Evento não encontrado")
    public EventoResponseDTO getId(@PathParam("id") Long id){
        return service.findById(id);
    }

    // Criação de eventos: admin da empresa ou gestor de evento.
    @POST
    @RolesAllowed({"SUPER_ADMIN", "ADMIN_EMPRESA", "GESTOR_EVENTO"})
    @Operation(summary = "Cria um novo evento")
    @APIResponse(responseCode = "201", description = "Evento criado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    public Response insert(EventoDTO eventoDTO){
        return service.insert(eventoDTO);
    }

    @POST
    @Path("/espacoevento")
    @RolesAllowed({"SUPER_ADMIN", "ADMIN_EMPRESA", "GESTOR_EVENTO"})
    @Operation(summary = "Cria um espaço dentro de um evento")
    @APIResponse(responseCode = "201", description = "Espaço de evento criado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    public Response insertEspacoEvento(EspacoEventoDTO espacoEventoDTO){
        return service.insertEspacoEvento(espacoEventoDTO);
    }

    // Exclusão (soft-delete): apenas admin da empresa.
    @PATCH
    @Path("/delete/{id}")
    @RolesAllowed({"SUPER_ADMIN", "ADMIN_EMPRESA"})
    @Operation(summary = "Soft-delete de um evento")
    @APIResponse(responseCode = "204", description = "Evento marcado como excluído")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "404", description = "Evento não encontrado")
    public void deleteById(@PathParam("id") Long id){
        service.deleteById(id);
    }

}
