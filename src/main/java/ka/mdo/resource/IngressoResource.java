package ka.mdo.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ka.mdo.qrcode.QrCodeService;
import ka.mdo.service.IngressoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/ingressos")
@Tag(name = "Ingressos", description = "Credenciais de acesso (QR Code)")
public class IngressoResource {

    private static final String SVG_MEDIA_TYPE = "image/svg+xml";

    @Inject
    IngressoService ingressoService;

    @Inject
    QrCodeService qrCodeService;

    @GET
    @Path("/{id}/qrcode")
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
}
