package ka.mdo.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import ka.mdo.dto.AutorizacaoDTO;
import ka.mdo.dto.AutorizacaoResponseDTO;
import ka.mdo.service.AutorizacaoEspacoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Endpoints da whitelist de tipos de ingresso autorizados por
 * {@code EspacoEvento} (atividade 030). Mudanças refletem imediatamente na
 * próxima validação em {@code AcessoService} — sem cache agressivo.
 *
 * <p><b>Autorização</b>: {@code GESTOR_EVENTO}, {@code ADMIN_EMPRESA},
 * {@code SUPER_ADMIN} e {@code GESTOR_LOCAL}. {@code GESTOR_LOCAL} só pode
 * operar sobre locais vinculados a ele via {@link ka.mdo.model.GestorLocal}
 * (atividade 041) — o service valida; fora dos locais vinculados é 403.
 */
@Path("/api/v1/espacos-evento/{espacoId}/autorizacoes")
@RolesAllowed({"GESTOR_EVENTO", "ADMIN_EMPRESA", "SUPER_ADMIN", "GESTOR_LOCAL"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Autorizações de local",
        description = "Whitelist de TipoIngresso autorizados a entrar em um EspacoEvento")
public class AutorizacaoEspacoResource {

    @Inject
    AutorizacaoEspacoService service;

    @GET
    @Operation(summary = "Lista os tipos de ingresso autorizados no local",
            description = "Lista vazia significa 'sem restrição' — qualquer credencial " +
                    "da empresa é autorizada no local (ver política em AcessoService).")
    @APIResponse(responseCode = "200", description = "Whitelist atual")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Espaço pertence a outra empresa")
    @APIResponse(responseCode = "404", description = "EspacoEvento inexistente")
    public AutorizacaoResponseDTO listar(@PathParam("espacoId") Long espacoId) {
        return service.listar(espacoId);
    }

    @PUT
    @Operation(summary = "Substitui a whitelist completa por uma nova lista",
            description = "Body: { tiposIngressoIds: [..] }. Use [] para remover todas " +
                    "as restrições. Registra uma entrada SUBSTITUIDO em AutorizacaoAuditoria.")
    @APIResponse(responseCode = "200", description = "Whitelist atualizada")
    @APIResponse(responseCode = "400", description = "Payload inválido")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Espaço ou tipo pertence a outra empresa")
    @APIResponse(responseCode = "404", description = "EspacoEvento ou TipoIngresso inexistente")
    public AutorizacaoResponseDTO substituir(@PathParam("espacoId") Long espacoId,
                                             @Valid AutorizacaoDTO dto) {
        return service.substituir(espacoId, dto.tiposIngressoIds());
    }

    @POST
    @Path("/{tipoIngressoId}")
    @Operation(summary = "Adiciona um TipoIngresso à whitelist do local",
            description = "Idempotente: se o tipo já estiver na lista, não duplica nem " +
                    "grava auditoria. Registra ADICIONADO em AutorizacaoAuditoria quando " +
                    "efetivamente muda a lista.")
    @APIResponse(responseCode = "200", description = "Whitelist atualizada")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Espaço ou tipo pertence a outra empresa")
    @APIResponse(responseCode = "404", description = "EspacoEvento ou TipoIngresso inexistente")
    public AutorizacaoResponseDTO adicionar(@PathParam("espacoId") Long espacoId,
                                            @PathParam("tipoIngressoId") Long tipoIngressoId) {
        return service.adicionar(espacoId, tipoIngressoId);
    }

    @DELETE
    @Path("/{tipoIngressoId}")
    @Operation(summary = "Remove um TipoIngresso da whitelist do local",
            description = "Idempotente: se o tipo não estiver na lista, não grava auditoria. " +
                    "Registra REMOVIDO em AutorizacaoAuditoria quando efetivamente muda a lista.")
    @APIResponse(responseCode = "200", description = "Whitelist atualizada")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Espaço ou tipo pertence a outra empresa")
    @APIResponse(responseCode = "404", description = "EspacoEvento ou TipoIngresso inexistente")
    public AutorizacaoResponseDTO remover(@PathParam("espacoId") Long espacoId,
                                          @PathParam("tipoIngressoId") Long tipoIngressoId) {
        return service.remover(espacoId, tipoIngressoId);
    }
}
