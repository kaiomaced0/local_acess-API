package ka.mdo.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import ka.mdo.dto.LogAcessoResponseDTO;
import ka.mdo.service.LogAcessoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoints de auditoria de acessos. Consumido por dashboards e telas de
 * resolução de disputas. Restrito a gestores/admins do tenant.
 *
 * <p>O filtro {@code tenantFilter} do Hibernate já isola os registros por
 * empresa — os parâmetros aceitos aqui são todos opcionais e servem para
 * narrow-down, nunca para "escolher o tenant".
 */
@Path("/logs-acesso")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN_EMPRESA", "GESTOR_EVENTO", "GESTOR_LOCAL", "SUPER_ADMIN"})
@Tag(name = "LogAcesso", description = "Auditoria de decisões de validação de credenciais")
public class LogAcessoResource {

    @Inject
    LogAcessoService logAcessoService;

    @GET
    @Operation(summary = "Lista logs de acesso da empresa autenticada",
            description = "Filtros opcionais: credencial, local e intervalo de datas. "
                    + "Ordenação fixa por dataHora DESC (mais recentes primeiro). "
                    + "GESTOR_LOCAL enxerga apenas logs dos locais vinculados a ele "
                    + "(ver entidade GestorLocal, atividade 041).")
    @APIResponse(responseCode = "200", description = "Lista paginada de logs")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    public List<LogAcessoResponseDTO> listar(
            @QueryParam("credencialId") Long credencialId,
            @QueryParam("localId") Long localId,
            @QueryParam("de") LocalDateTime de,
            @QueryParam("ate") LocalDateTime ate,
            @QueryParam("pagina") Integer pagina,
            @QueryParam("tamanho") Integer tamanho) {

        // Atividade 041: restrição por vínculo GestorLocal aplicada no service.
        int p = pagina == null ? 0 : pagina;
        int t = tamanho == null ? 20 : tamanho;
        return logAcessoService.buscar(credencialId, localId, de, ate, p, t);
    }
}
