package ka.mdo.storage;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementação de {@link StorageService} baseada no cliente MinIO (compatível com S3).
 * Não deve ser referenciada diretamente por resources; injete a interface.
 */
@ApplicationScoped
public class MinioStorageService implements StorageService {

    @Inject
    MinioClient minioClient;

    @ConfigProperty(name = "storage.presigned-url-ttl-seconds", defaultValue = "300")
    int presignedUrlTtlSeconds;

    @Override
    public String upload(String bucket, String nomeObjeto, byte[] conteudo, String contentType) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(conteudo)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(nomeObjeto)
                            .stream(stream, conteudo.length, -1)
                            .contentType(contentType)
                            .build());
            return nomeObjeto;
        } catch (Exception e) {
            throw new StorageException("Falha ao fazer upload do objeto " + nomeObjeto + " no bucket " + bucket, e);
        }
    }

    @Override
    public String downloadUrl(String bucket, String nomeObjeto) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(nomeObjeto)
                            .expiry(presignedUrlTtlSeconds, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            throw new StorageException("Falha ao gerar URL assinada para " + nomeObjeto, e);
        }
    }

    @Override
    public void delete(String bucket, String nomeObjeto) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(nomeObjeto)
                            .build());
        } catch (Exception e) {
            throw new StorageException("Falha ao remover objeto " + nomeObjeto + " do bucket " + bucket, e);
        }
    }
}
