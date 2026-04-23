package ka.mdo.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import ka.mdo.dto.PendenciaResponseDTO;
import ka.mdo.dto.ResolucaoPendenciaDTO;
import ka.mdo.model.Pendencia;
import ka.mdo.model.StatusPendencia;
import ka.mdo.pendencia.PendenciaService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * Endpoints do workflow de pendências (atividade 031). Usados pela fila do
 * gestor para analisar e resolver pendências de acesso.
 *
 * <p>Acesso restrito a perfis gestores/admins. O filtro {@code tenantFilter}
 * do Hibernate isola por empresa automaticamente — nenhum parâmetro de
 * {@code empresaId} é aceito (o tenant vem do JWT).
 */
@Path("/api/v1/pendencias")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"GESTOR_EVENTO", "GESTOR_LOCAL", "ADMIN_EMPRESA", "SUPER_ADMIN"})
@Tag(name = "Pendencia", description = "Workflow de pendências de acesso")
public class PendenciaResource {

    @Inject
    PendenciaService pendenciaService;

    @GET
    @Operation(summary = "Fila de pendências do tenant",
            description = "Filtros opcionais por status e local. Ordenação fixa por criadaEm DESC. "
                    + "GESTOR_LOCAL enxerga apenas pendências dos locais vinculados a ele "
                    + "(entidade GestorLocal, atividade 041) — pendências sem local também.")
    @APIResponse(responseCode = "200", description = "Lista paginada de pendências")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    public List<PendenciaResponseDTO> listar(
            @QueryParam("status") StatusPendencia status,
            @QueryParam("localId") Long localId,
            @QueryParam("pagina") Integer pagina,
            @QueryParam("tamanho") Integer tamanho) {
        int p = pagina == null ? 0 : pagina;
        int t = tamanho == null ? 20 : tamanho;
        return pendenciaService.buscar(status, localId, p, t);
    }

    @POST
    @Path("/{id}/aprovar")
    @Operation(summary = "Aprova uma pendência",
            description = "Idempotente: se a pendência já estava resolvida, devolve o estado atual. "
                    + "Para pendências faciais, a aprovação reseta "
                    + "DadosPessoais.rostoFrigateCadastrado para que a próxima leitura re-enrole o "
                    + "rosto automaticamente.")
    @APIResponse(responseCode = "200", description = "Pendência aprovada ou já resolvida")
    @APIResponse(responseCode = "403", description = "Tenant da pendência difere do JWT")
    @APIResponse(responseCode = "404", description = "Pendência não encontrada")
    public PendenciaResponseDTO aprovar(@PathParam("id") Long id,
                                        @Valid ResolucaoPendenciaDTO body) {
        String obs = body == null ? null : body.observacao();
        Pendencia p = pendenciaService.aprovar(id, obs);
        return pendenciaService.toResponse(p);
    }

    @POST
    @Path("/{id}/recusar")
    @Operation(summary = "Recusa uma pendência",
            description = "Idempotente. Notifica o dono da credencial.")
    @APIResponse(responseCode = "200", description = "Pendência recusada ou já resolvida")
    @APIResponse(responseCode = "403", description = "Tenant da pendência difere do JWT")
    @APIResponse(responseCode = "404", description = "Pendência não encontrada")
    public PendenciaResponseDTO recusar(@PathParam("id") Long id,
                                        @Valid ResolucaoPendenciaDTO body) {
        String obs = body == null ? null : body.observacao();
        Pendencia p = pendenciaService.recusar(id, obs);
        return pendenciaService.toResponse(p);
    }
}
