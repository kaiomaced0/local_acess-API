package ka.mdo.resource;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import ka.mdo.dto.DadosPessoaisDTO;
import ka.mdo.dto.DadosPessoaisResponseDTO;
import ka.mdo.service.DadosPessoaisService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Endpoints de dados pessoais do dono da credencial (atividade 020).
 *
 * <p>Upload de imagem: usamos {@code application/octet-stream} com header
 * {@code X-Content-Type} para evitar uma dependência nova de multipart
 * (quarkus-resteasy-multipart). O cliente envia os bytes crus e informa o
 * MIME no header; o service chama {@code StorageValidator.validarImagem}
 * antes de persistir.
 */
@Path("/api/v1/dados-pessoais")
@Authenticated
@Tag(name = "Dados Pessoais", description = "Dados pessoais e foto do dono da credencial")
public class DadosPessoaisResource {

    @Inject
    DadosPessoaisService service;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cria ou atualiza os dados pessoais do usuário logado (upsert)")
    @APIResponse(responseCode = "200", description = "Dados salvos")
    @APIResponse(responseCode = "400", description = "CPF inválido ou documento duplicado")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    public DadosPessoaisResponseDTO salvarMeus(@Valid DadosPessoaisDTO dto) {
        return service.salvarParaUsuarioLogado(dto);
    }

    @GET
    @Path("/meus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retorna os dados pessoais do usuário logado")
    @APIResponse(responseCode = "200", description = "Dados encontrados")
    @APIResponse(responseCode = "404", description = "Dados ainda não preenchidos")
    public DadosPessoaisResponseDTO meus() {
        return service.buscarDoUsuarioLogado();
    }

    /**
     * Consulta por id. Restrito a gestores do tenant. Quando
     * {@code incluirDocumento=true}, retorna o documento em claro no campo
     * {@code documentoMascarado} — usado apenas para auditoria de gestor.
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SUPER_ADMIN", "ADMIN_EMPRESA", "GESTOR_EVENTO", "GESTOR_LOCAL"})
    @Operation(summary = "Busca dados pessoais por id (gestores)")
    @APIResponse(responseCode = "200", description = "Dados encontrados")
    @APIResponse(responseCode = "403", description = "Sem permissão")
    @APIResponse(responseCode = "404", description = "Não encontrado")
    public DadosPessoaisResponseDTO getById(
            @PathParam("id") Long id,
            @QueryParam("incluirDocumento") @DefaultValue("false") boolean incluirDocumento) {
        return service.buscarPorId(id, incluirDocumento);
    }

    /**
     * Upload da selfie (foto do dono da credencial). Envie o binário cru no
     * corpo com {@code Content-Type: application/octet-stream} e o MIME real
     * (ex: {@code image/jpeg}) no header {@code X-Content-Type}.
     */
    @POST
    @Path("/{id}/foto")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload da foto do dono da credencial")
    @APIResponse(responseCode = "200", description = "Foto enviada")
    @APIResponse(responseCode = "400", description = "Arquivo inválido")
    @APIResponse(responseCode = "403", description = "Sem permissão")
    @APIResponse(responseCode = "404", description = "Dados pessoais não encontrados")
    public DadosPessoaisResponseDTO uploadFoto(
            @PathParam("id") Long id,
            @HeaderParam("X-Content-Type") String contentType,
            byte[] bytes) {
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("Header X-Content-Type obrigatório (ex: image/jpeg)");
        }
        return service.uploadFoto(id, bytes, contentType);
    }

    @POST
    @Path("/{id}/documento-foto")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload da foto do documento de identificação")
    @APIResponse(responseCode = "200", description = "Foto do documento enviada")
    @APIResponse(responseCode = "400", description = "Arquivo inválido")
    @APIResponse(responseCode = "403", description = "Sem permissão")
    @APIResponse(responseCode = "404", description = "Dados pessoais não encontrados")
    public DadosPessoaisResponseDTO uploadDocumentoFoto(
            @PathParam("id") Long id,
            @HeaderParam("X-Content-Type") String contentType,
            byte[] bytes) {
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("Header X-Content-Type obrigatório (ex: image/jpeg)");
        }
        return service.uploadDocumentoFoto(id, bytes, contentType);
    }
}
