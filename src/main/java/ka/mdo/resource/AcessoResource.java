package ka.mdo.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import ka.mdo.dto.ValidarAcessoRequestDTO;
import ka.mdo.dto.ValidarAcessoResponseDTO;
import ka.mdo.service.AcessoService;

/**
 * Endpoints consumidos pelos aparelhos de validação (totens, leitores,
 * tablets) operados pelo perfil {@code OPERADOR_APARELHO}.
 */
@Path("/acesso")
@RolesAllowed("OPERADOR_APARELHO")
@Tag(name = "Acesso", description = "Validação de credenciais lidas por aparelhos")
public class AcessoResource {

    private static final Logger LOG = Logger.getLogger(AcessoResource.class);

    @Inject
    AcessoService acessoService;

    /**
     * Endpoint clássico (atividade 011). Mantido sem alteração para compat
     * com aparelhos já no campo — eventos com {@code validarFacial=true}
     * receberão {@code PENDENTE FOTO_FACIAL_AUSENTE} orientando o aparelho
     * a reenviar via {@link #validarComFoto}.
     */
    @POST
    @Path("/validar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Valida um acesso a partir do token lido pelo aparelho",
            description = "Recebe o token (payload do QR Code), o id do aparelho que está " +
                    "fazendo a leitura e, opcionalmente, o id do local onde ele está " +
                    "posicionado. Devolve AUTORIZADO/PENDENTE/NEGADO com motivo.")
    @APIResponse(responseCode = "200", description = "Decisão calculada (inclusive NEGADO)")
    @APIResponse(responseCode = "400", description = "Payload inválido")
    @APIResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Aparelho/credencial de outra empresa")
    public ValidarAcessoResponseDTO validar(@Valid ValidarAcessoRequestDTO req) {
        return acessoService.validar(req);
    }

    /**
     * Endpoint com foto capturada (atividade 021) para eventos com
     * {@code validarFacial=true}. Usamos {@code application/octet-stream}
     * com os parâmetros de contexto em query/header — mesmo padrão do upload
     * de fotos em {@code DadosPessoaisResource} — para evitar dependência
     * multipart e manter o client dos aparelhos simples.
     *
     * <ul>
     *     <li>Body: bytes crus da imagem JPEG/PNG.</li>
     *     <li>Header {@code X-Content-Type}: MIME real (default
     *     {@code image/jpeg}).</li>
     *     <li>Query {@code token}, {@code aparelhoId}, {@code localId} (opcional).</li>
     * </ul>
     * Eventos que NÃO exigem validação facial também aceitam este endpoint —
     * a foto é simplesmente ignorada (continua compat). O comportamento
     * coincide com {@link #validar} neste caso.
     */
    @POST
    @Path("/validar-com-foto")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Valida acesso enviando também a foto capturada pelo aparelho",
            description = "Endpoint dedicado a eventos com validação facial (atividade 021). " +
                    "Recebe os bytes da imagem no body (octet-stream), os parâmetros de " +
                    "contexto em query e o MIME real em X-Content-Type. Sem foto, eventos " +
                    "com validarFacial=true devolvem PENDENTE FOTO_FACIAL_AUSENTE.")
    @APIResponse(responseCode = "200", description = "Decisão calculada (inclusive NEGADO)")
    @APIResponse(responseCode = "400", description = "Imagem inválida ou payload faltando")
    @APIResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Aparelho/credencial de outra empresa")
    public ValidarAcessoResponseDTO validarComFoto(
            @QueryParam("token") @NotBlank String token,
            @QueryParam("aparelhoId") @NotNull Long aparelhoId,
            @QueryParam("localId") Long localId,
            @HeaderParam("X-Content-Type") String contentType,
            byte[] fotoCapturada) {

        if (fotoCapturada == null || fotoCapturada.length == 0) {
            // Deixamos o service decidir: pode ser que o evento nem exija
            // foto. Entregamos null explícito.
            fotoCapturada = null;
        } else {
            LOG.debugf("validarComFoto: aparelho=%d bytes=%d",
                    aparelhoId.longValue(), (long) fotoCapturada.length);
        }
        
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM.equals("image/jpeg") ? "image/jpeg" : "image/jpeg";
        }

        ValidarAcessoRequestDTO req = new ValidarAcessoRequestDTO(token, localId, aparelhoId);
        return acessoService.validarComFoto(req, fotoCapturada, contentType);
    }
}
