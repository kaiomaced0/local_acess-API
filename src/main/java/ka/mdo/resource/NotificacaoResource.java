package ka.mdo.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.NotificacaoResponseDTO;
import ka.mdo.service.NotificacaoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de notificações do usuário logado (atividade 032). Qualquer
 * perfil autenticado pode acessar — cada usuário só enxerga as próprias
 * notificações (service valida via subject do JWT).
 */
@Path("/notificacoes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Notificacao", description = "Notificações do usuário logado")
public class NotificacaoResource {

    @Inject
    NotificacaoService notificacaoService;

    @GET
    @Operation(summary = "Lista notificações do usuário logado",
            description = "Paginação opcional via query params. Ordenação fixa por criadaEm DESC.")
    @APIResponse(responseCode = "200", description = "Lista paginada de notificações")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    public List<NotificacaoResponseDTO> listar(
            @QueryParam("pagina") Integer pagina,
            @QueryParam("tamanho") Integer tamanho) {
        int p = pagina == null ? 0 : pagina;
        int t = tamanho == null ? 20 : tamanho;
        return notificacaoService.listarDoLogado(p, t);
    }

    @POST
    @Path("/{id}/lida")
    @Operation(summary = "Marca uma notificação como lida",
            description = "Só o próprio destinatário pode marcar.")
    @APIResponse(responseCode = "204", description = "Marcada com sucesso")
    @APIResponse(responseCode = "403", description = "Notificação não pertence ao logado")
    @APIResponse(responseCode = "404", description = "Notificação não encontrada")
    public Response marcarLida(@PathParam("id") Long id) {
        notificacaoService.marcarLida(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/nao-lidas/count")
    @Operation(summary = "Conta notificações não lidas do logado",
            description = "Usado pelo painel para o badge de notificações.")
    @APIResponse(responseCode = "200", description = "Total de não lidas")
    public Map<String, Long> contarNaoLidas() {
        return Map.of("total", notificacaoService.contarNaoLidas());
    }
}
