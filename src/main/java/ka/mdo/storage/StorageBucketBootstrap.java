package ka.mdo.storage;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * No startup, garante que os buckets configurados existam.
 * Apenas cria; nunca apaga. Erros de criação são logados mas não impedem o startup
 * para não acoplar rigidamente o boot da API à disponibilidade do MinIO.
 */
@ApplicationScoped
public class StorageBucketBootstrap {

    private static final Logger LOG = Logger.getLogger(StorageBucketBootstrap.class);

    @Inject
    MinioClient minioClient;

    @ConfigProperty(name = "storage.bucket.credenciais-foto")
    String bucketCredenciaisFoto;

    @ConfigProperty(name = "storage.bucket.documentos")
    String bucketDocumentos;

    @ConfigProperty(name = "storage.bucket.capturas-acesso")
    String bucketCapturasAcesso;

    /**
     * Bucket para imagens de fundo do mapa 2D do evento (atividade 040).
     * Default mantido no {@code @ConfigProperty} para que o bootstrap rode
     * mesmo quando a chave não foi explicitamente definida no
     * {@code application.properties}.
     */
    @ConfigProperty(name = "storage.bucket.mapas", defaultValue = "mapas")
    String bucketMapas;

    void onStart(@Observes StartupEvent event) {
        List<String> buckets = List.of(bucketCredenciaisFoto, bucketDocumentos, bucketCapturasAcesso, bucketMapas);
        for (String bucket : buckets) {
            criarSeNaoExistir(bucket);
        }
    }

    private void criarSeNaoExistir(String bucket) {
        try {
            boolean existe = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!existe) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                LOG.infof("Bucket '%s' criado no storage.", bucket);
            } else {
                LOG.debugf("Bucket '%s' já existe.", bucket);
            }
        } catch (Exception e) {
            LOG.warnf(e, "Não foi possível garantir o bucket '%s'. Uploads para esse bucket podem falhar.", bucket);
        }
    }
}
