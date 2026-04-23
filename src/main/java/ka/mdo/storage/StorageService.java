package ka.mdo.storage;

/**
 * Abstração para armazenamento de objetos (imagens, documentos, capturas).
 * Implementações devem isolar o SDK concreto (MinIO/S3) do restante da aplicação.
 */
public interface StorageService {

    /**
     * Faz upload de um objeto ao bucket informado.
     *
     * @param bucket       nome do bucket de destino
     * @param nomeObjeto   chave do objeto (pode conter prefixos com "/")
     * @param conteudo     bytes do arquivo
     * @param contentType  MIME type (ex: image/jpeg)
     * @return chave do objeto persistido (igual a {@code nomeObjeto})
     */
    String upload(String bucket, String nomeObjeto, byte[] conteudo, String contentType);

    /**
     * Gera URL pré-assinada para leitura temporária do objeto.
     * TTL controlado por {@code storage.presigned-url-ttl-seconds}.
     */
    String downloadUrl(String bucket, String nomeObjeto);

    /**
     * Remove o objeto do bucket.
     */
    void delete(String bucket, String nomeObjeto);
}
