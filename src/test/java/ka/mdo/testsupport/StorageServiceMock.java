package ka.mdo.testsupport;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.storage.StorageService;

/**
 * Mock do {@link StorageService} (MinIO) para os testes de integração
 * (atividade 051): evita qualquer chamada ao object storage real.
 */
@Mock
@ApplicationScoped
public class StorageServiceMock implements StorageService {

    @Override
    public String upload(String bucket, String nomeObjeto, byte[] conteudo, String contentType) {
        return nomeObjeto;
    }

    @Override
    public String downloadUrl(String bucket, String nomeObjeto) {
        return "https://storage.test/" + bucket + "/" + nomeObjeto;
    }

    @Override
    public void delete(String bucket, String nomeObjeto) {
        // no-op
    }
}
