package ka.mdo.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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
import ka.mdo.dto.AparelhoDTO;
import ka.mdo.dto.AparelhoResponseDTO;
import ka.mdo.service.AparelhoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * CRUD de {@link ka.mdo.model.Aparelho} (atividade 014). Restrito a perfis de
 * gestão do tenant (ADMIN_EMPRESA / SUPER_ADMIN); o painel do gestor é o único
 * consumidor.
 *
 * <p>O isolamento por tenant é garantido pelo {@code tenantFilter} do
 * Hibernate (ver {@link ka.mdo.tenant.TenantRequestFilter}). Para vínculos com
 * {@code eventoId} e {@code localEspecificoId} o service valida que os
 * recursos pertencem ao mesmo tenant — 403 caso contrário.
 */
@Path("/aparelhos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN_EMPRESA", "SUPER_ADMIN"})
@Tag(name = "Aparelhos", description = "Gestão de dispositivos de validação da empresa")
public class AparelhoResource {

    @Inject
    AparelhoService service;

    @GET
    @Operation(summary = "Lista aparelhos da empresa",
            description = "Paginado. Filtros opcionais: ativo, eventoId, localEspecificoId.")
    @APIResponse(responseCode = "200", description = "Lista paginada de aparelhos")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    public List<AparelhoResponseDTO> listar(
            @QueryParam("ativo") Boolean ativo,
            @QueryParam("eventoId") Long eventoId,
            @QueryParam("localEspecificoId") Long localEspecificoId,
            @QueryParam("pagina") Integer pagina,
            @QueryParam("tamanho") Integer tamanho) {
        int p = pagina == null ? 0 : pagina;
        int t = tamanho == null ? 20 : tamanho;
        return service.listar(ativo, eventoId, localEspecificoId, p, t);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Detalhe de um aparelho")
    @APIResponse(responseCode = "200", description = "Aparelho encontrado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "404", description = "Aparelho não encontrado")
    public AparelhoResponseDTO detalhar(@PathParam("id") Long id) {
        return service.buscarPorId(id);
    }

    @POST
    @Operation(summary = "Cria um aparelho",
            description = "Empresa vem do JWT. eventoId e localEspecificoId opcionais — quando informados, devem pertencer ao tenant.")
    @APIResponse(responseCode = "201", description = "Aparelho criado")
    @APIResponse(responseCode = "400", description = "Payload inválido")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Recurso vinculado pertence a outra empresa")
    public Response criar(@Valid AparelhoDTO dto) {
        AparelhoResponseDTO criado = service.criar(dto);
        return Response.status(Response.Status.CREATED).entity(criado).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza descrição e vínculos do aparelho",
            description = "Não altera empresa nem estado ativo. Para toggle de ativo use os endpoints /desativar e /reativar.")
    @APIResponse(responseCode = "200", description = "Aparelho atualizado")
    @APIResponse(responseCode = "400", description = "Payload inválido")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Recurso vinculado pertence a outra empresa")
    @APIResponse(responseCode = "404", description = "Aparelho não encontrado")
    public AparelhoResponseDTO atualizar(@PathParam("id") Long id, @Valid AparelhoDTO dto) {
        return service.atualizar(id, dto);
    }

    @PATCH
    @Path("/{id}/desativar")
    @Operation(summary = "Desativa o aparelho",
            description = "Aparelho inativo retorna NEGADO em /acesso/validar (ver AcessoService).")
    @APIResponse(responseCode = "200", description = "Aparelho desativado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "404", description = "Aparelho não encontrado")
    public AparelhoResponseDTO desativar(@PathParam("id") Long id) {
        return service.desativar(id);
    }

    @PATCH
    @Path("/{id}/reativar")
    @Operation(summary = "Reativa o aparelho")
    @APIResponse(responseCode = "200", description = "Aparelho reativado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "404", description = "Aparelho não encontrado")
    public AparelhoResponseDTO reativar(@PathParam("id") Long id) {
        return service.reativar(id);
    }
}
