package ka.mdo.resource;

import ka.mdo.dto.UsuarioDTO;
import ka.mdo.dto.UsuarioResponseDTO;
import ka.mdo.model.EntityClass;
import ka.mdo.model.Usuario;
import ka.mdo.service.UsuarioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/usuario")
public class UsuarioResource {
    @Inject
    private UsuarioService service;


    @GET
    public List<UsuarioResponseDTO> getAll(){
        return service.findAll();
    }
    @GET
    @Path("/{id}")
    public UsuarioResponseDTO getById(@PathParam("id") Long id) {
        return service.findById(id);
    }

    @POST
    public Response create(UsuarioDTO entity) {
        return service.insert(entity);
    }

    @PUT
    @Path("/{id}")
    public Usuario update(@PathParam("id") Long id, Usuario entity) {
        return service.update(entity);
    }

    @PATCH
    @Path("/delete/{id}")
    public void delete(@PathParam("id") Long id) {
        service.deleteById(id);
    }
}
