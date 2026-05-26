package ka.mdo.resource;

import jakarta.annotation.security.RolesAllowed;
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
import ka.mdo.dto.IngressoDTO;
import ka.mdo.qrcode.QrCodeService;
import ka.mdo.service.IngressoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Ingressos", description = "Credenciais de acesso (QR Code)")
public class IngressoResource {

    private static final String SVG_MEDIA_TYPE = "image/svg+xml";

    @Inject
    IngressoService ingressoService;

    @Inject
    QrCodeService qrCodeService;

    @GET
    @Path("/ingressos/{id}/qrcode")
    @Produces({"image/png", SVG_MEDIA_TYPE})
    @RolesAllowed({"CLIENTE", "ADMIN_EMPRESA", "GESTOR_EVENTO", "GESTOR_LOCAL", "SUPER_ADMIN"})
    @Operation(summary = "Gera o QR Code da credencial",
            description = "Retorna imagem PNG (default) ou SVG (formato=svg) do QR derivado do token opaco da credencial. Cliente só consegue gerar o QR da própria credencial; gestores da empresa podem gerar de qualquer credencial do tenant.")
    @APIResponse(responseCode = "200", description = "QR Code renderizado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Credencial pertence a outra empresa/usuário")
    @APIResponse(responseCode = "404", description = "Credencial não encontrada")
    public Response qrCode(@PathParam("id") Long id,
                           @QueryParam("formato") String formato,
                           @QueryParam("tamanho") Integer tamanho) {
        String token = ingressoService.tokenParaQrCode(id);
        int dim = (tamanho == null || tamanho <= 0) ? QrCodeService.TAMANHO_DEFAULT : tamanho;

        if ("svg".equalsIgnoreCase(formato)) {
            byte[] svg = qrCodeService.gerarSvg(token, dim);
            return Response.ok(svg)
                    .type(SVG_MEDIA_TYPE)
                    .header("Cache-Control", "no-store")
                    .build();
        }

        byte[] png = qrCodeService.gerarPng(token, dim);
        return Response.ok(png)
                .type("image/png")
                .header("Cache-Control", "no-store")
                .build();
    }

    /**
     * Atividade 013: emissão de credencial para um usuário do tenant.
     *
     * <p>Path documentado em {@code PERMISSIONS.md} como
     * {@code POST /usuarios/{id}/ingressos} (sub-resource de usuários, ainda
     * que vivido em {@code IngressoResource} para colocar a lógica de
     * credencial em um único arquivo). Delega ao
     * {@link IngressoService#adicionarIngresso(Long, IngressoDTO)} — que já
     * aplica isolamento por tenant (403 cross-tenant) e o gate de
     * {@code escopoGlobal} da atividade 033.
     *
     * <p>Resposta {@code 201 Created} com {@link ka.mdo.dto.IngressoResponseDTO}
     * (nunca expõe o token bruto — só o id, usado para gerar o QR).
     */
    @POST
    @Path("/usuarios/{idUsuario}/ingressos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN_EMPRESA", "GESTOR_EVENTO", "SUPER_ADMIN"})
    @Operation(summary = "Emite uma credencial para o usuário",
            description = "Cria um Ingresso vinculado ao usuário do tenant, gera o token opaco (base do QR) e devolve o IngressoResponseDTO. Aceita o campo opcional escopoGlobal (atividade 033) com o gate de papel já aplicado no service.")
    @APIResponse(responseCode = "201", description = "Credencial emitida")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Usuário/TipoIngresso de outro tenant, ou perfil sem direito de emitir credencial global")
    public Response emitir(@PathParam("idUsuario") Long idUsuario, IngressoDTO dto) {
        return ingressoService.adicionarIngresso(idUsuario, dto);
    }
}
