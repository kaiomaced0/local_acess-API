package ka.mdo.storage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

/**
 * Valida tamanho e MIME type antes do upload.
 * Deve ser invocado por quem recebe o arquivo (resource/service) ANTES de chamar {@link StorageService#upload}.
 */
@ApplicationScoped
public class StorageValidator {

    @ConfigProperty(name = "storage.max-file-size-bytes", defaultValue = "5242880")
    long maxFileSizeBytes;

    @ConfigProperty(name = "storage.tipos-permitidos-imagem", defaultValue = "image/jpeg,image/png,image/webp")
    String tiposPermitidosImagemRaw;

    /**
     * Valida bytes e content-type de uma imagem candidata a upload.
     *
     * @throws BadRequestException se tamanho exceder o limite ou content-type não estiver autorizado.
     */
    public void validarImagem(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("Arquivo vazio");
        }
        if (bytes.length > maxFileSizeBytes) {
            throw new BadRequestException(
                    "Arquivo excede o tamanho máximo permitido (" + maxFileSizeBytes + " bytes)");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("Content-Type obrigatório");
        }
        List<String> permitidos = tiposPermitidos();
        String normalizado = contentType.trim().toLowerCase();
        if (!permitidos.contains(normalizado)) {
            throw new BadRequestException(
                    "Tipo de imagem não permitido: " + contentType + ". Aceitos: " + String.join(", ", permitidos));
        }
    }

    private List<String> tiposPermitidos() {
        return Arrays.stream(tiposPermitidosImagemRaw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
