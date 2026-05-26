package ka.mdo.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.TipoIngressoDTO;
import ka.mdo.dto.TipoIngressoResponseDTO;
import ka.mdo.service.TipoIngressoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * CRUD de TipoIngresso (atividade 015). Endpoints isolados por tenant via
 * {@code tenantFilter} do Hibernate — o {@code empresaId} vem do JWT,
 * nunca da requisição.
 */
@Path("/tipos-ingresso")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN_EMPRESA", "GESTOR_EVENTO", "SUPER_ADMIN"})
@Tag(name = "Tipos de Ingresso", description = "CRUD de tipos de ingresso da empresa (atividade 015)")
public class TipoIngressoResource {

    @Inject
    TipoIngressoService service;

    @GET
    @Operation(summary = "Lista tipos de ingresso da empresa",
            description = "Por default retorna apenas registros ativos. Use ?incluirInativos=true para incluir os soft-deletados.")
    @APIResponse(responseCode = "200", description = "Lista paginada de tipos de ingresso")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    public List<TipoIngressoResponseDTO> listar(
            @QueryParam("incluirInativos") @DefaultValue("false") boolean incluirInativos,
            @QueryParam("pagina") @DefaultValue("0") int pagina,
            @QueryParam("tamanho") @DefaultValue("20") int tamanho) {
        return service.listar(incluirInativos, pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca um tipo de ingresso por id")
    @APIResponse(responseCode = "200", description = "TipoIngresso encontrado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Recurso pertence a outra empresa")
    @APIResponse(responseCode = "404", description = "TipoIngresso não encontrado")
    public TipoIngressoResponseDTO buscar(@PathParam("id") Long id) {
        return service.buscarPorId(id);
    }

    @POST
    @Operation(summary = "Cria um novo tipo de ingresso",
            description = "Nome único por empresa. Retorna 409 se já existir tipo ativo com o mesmo nome.")
    @APIResponse(responseCode = "201", description = "TipoIngresso criado")
    @APIResponse(responseCode = "400", description = "Payload inválido")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "409", description = "Já existe TipoIngresso ativo com esse nome")
    public Response criar(@Valid TipoIngressoDTO dto) {
        return service.criar(dto);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza o nome de um tipo de ingresso",
            description = "Nome continua único por empresa. Retorna 409 se houver conflito.")
    @APIResponse(responseCode = "200", description = "TipoIngresso atualizado")
    @APIResponse(responseCode = "400", description = "Payload inválido")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Recurso pertence a outra empresa")
    @APIResponse(responseCode = "404", description = "TipoIngresso não encontrado")
    @APIResponse(responseCode = "409", description = "Já existe outro TipoIngresso ativo com esse nome")
    public TipoIngressoResponseDTO atualizar(@PathParam("id") Long id, @Valid TipoIngressoDTO dto) {
        return service.atualizar(id, dto);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Soft-delete de um tipo de ingresso",
            description = "Marca ativo=false. Bloqueia (409) se existirem Ingressos ativos referenciando este tipo.")
    @APIResponse(responseCode = "204", description = "TipoIngresso desativado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Recurso pertence a outra empresa")
    @APIResponse(responseCode = "404", description = "TipoIngresso não encontrado")
    @APIResponse(responseCode = "409", description = "Existem credenciais ativas vinculadas a este tipo")
    public Response deletar(@PathParam("id") Long id) {
        service.deletar(id);
        return Response.noContent().build();
    }
}
