package ka.mdo.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.EspacoEventoDTO;
import ka.mdo.dto.EventoDTO;
import ka.mdo.dto.EventoResponseDTO;
import ka.mdo.service.EventoService;

import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/evento")
public class EventoResource {

    @Inject
    private EventoService service;

    @GET
    public List<EventoResponseDTO> getAll(){
        return service.findAll();
    }

    @GET
    @Path("/{id}")
    public EventoResponseDTO getId(@PathParam("id") Long id){
        return service.findById(id);
    }

    @POST
    public Response insert(EventoDTO eventoDTO){
        return service.insert(eventoDTO);
    }

    @POST
    @Path("/espacoevento")
    public Response insertEspacoEvento(EspacoEventoDTO espacoEventoDTO){
        return service.insertEspacoEvento(espacoEventoDTO);
    }

    @PATCH
    @Path("/delete/{id}")
    public void deleteById(@PathParam("id") Long id){
        service.deleteById(id);
    }

}
