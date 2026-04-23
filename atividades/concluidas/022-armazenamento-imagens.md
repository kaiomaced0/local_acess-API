---
id: 022
titulo: Armazenamento de imagens (MinIO/S3)
prioridade: alta
estimativa: M
depende-de: []
epico: pessoa
---

## Contexto
Fotos de credenciais, documentos e capturas dos aparelhos não devem ficar no banco de dados.

## Objetivo
Serviço de storage com buckets separados, URLs assinadas, retenção configurável.

## Critérios de aceitação
- [ ] Dependência `quarkus-minio` (ou `quarkus-amazon-s3` para AWS) adicionada.
- [ ] Interface `StorageService` com `upload`, `downloadUrl`, `delete`.
- [ ] Buckets/prefixos separados: `credenciais-foto/`, `documentos/`, `capturas-acesso/`.
- [ ] URLs pré-assinadas com TTL curto para acesso do cliente.
- [ ] Tamanho máximo e tipos permitidos validados antes do upload.
- [ ] Teste de integração com MinIO em container (Testcontainers).

## Notas técnicas
- Criptografia em repouso habilitada (server-side encryption).
- Para documentos, avaliar criptografia client-side adicional.

## Resultado

### Implementação
Infraestrutura de storage S3-compatível via MinIO. Nada de lógica de negócio ou endpoint novo — só camada utilitária para as atividades 012/020/021 consumirem.

### Arquivos criados
- `src/main/java/ka/mdo/storage/StorageService.java` — interface pública (`upload`, `downloadUrl`, `delete`).
- `src/main/java/ka/mdo/storage/MinioStorageService.java` — implementação `@ApplicationScoped` sobre `io.minio.MinioClient` (`PutObjectArgs`, `GetPresignedObjectUrlArgs`, `RemoveObjectArgs`).
- `src/main/java/ka/mdo/storage/StorageException.java` — RuntimeException para falhas de I/O do backend.
- `src/main/java/ka/mdo/storage/StorageValidator.java` — valida tamanho e MIME type via `storage.max-file-size-bytes` / `storage.tipos-permitidos-imagem`, lança `BadRequestException`.
- `src/main/java/ka/mdo/storage/StorageBucketBootstrap.java` — `@Observes StartupEvent`, cria buckets `credenciais-foto`, `documentos`, `capturas-acesso` se não existirem; falha silenciosa (log warn) para não acoplar boot da API ao MinIO.

### Arquivos alterados
- `pom.xml` — adicionada dependência `io.quarkiverse.minio:quarkus-minio:3.3.1` (única adição do escopo 022). A versão 3.4.0 não existe no Maven Central; 3.3.1 é a última da família 3.3.x e é retrocompatível com Quarkus 3.5.3.
- `src/main/resources/application.properties` — adicionado bloco `quarkus.minio.*` (url, access-key, secret-key) e `storage.*` (3 nomes de bucket, TTL de URL assinada, tamanho máximo, tipos MIME permitidos). Credenciais padrão só para dev; em produção vêm de variáveis de ambiente (`QUARKUS_MINIO_URL`, `QUARKUS_MINIO_ACCESS_KEY`, `QUARKUS_MINIO_SECRET_KEY`).

### Ajuste de versão
`quarkus-minio` 3.4.0 solicitado no enunciado não existe no Maven Central. Versões 3.x disponíveis: 3.0.x, 3.1.0.Final, 3.3.1, 3.7.x, 3.8.1. Usada 3.3.1 (alinhada a Quarkus 3.3/3.5) por ser a mais próxima e estável. Se no futuro migrarmos para Quarkus 3.7+, subir para `quarkus-minio:3.7.7` ou `3.8.1`.

### Build
`./mvnw.cmd compile` — BUILD SUCCESS.

### Critérios de aceitação
- [x] Dependência `quarkus-minio` adicionada (3.3.1).
- [x] Interface `StorageService` com `upload`, `downloadUrl`, `delete`.
- [x] Buckets separados: `credenciais-foto`, `documentos`, `capturas-acesso` (criados no startup).
- [x] URLs pré-assinadas com TTL curto (300s configurável via `storage.presigned-url-ttl-seconds`).
- [x] Tamanho máximo e tipos permitidos validados (`StorageValidator`, `BadRequestException`).
- [ ] Teste de integração com MinIO em Testcontainers — **deferido para 051** (requer dependência nova `org.testcontainers:minio` e configuração de `@QuarkusTestResource`; ultrapassa o orçamento de 50 linhas do escopo).

### Notas para consumidores
- Resources nunca devem injetar `MinioClient` diretamente; usar `StorageService`.
- Antes de chamar `upload`, invocar `StorageValidator.validarImagem(bytes, contentType)`.
- Nome do bucket deve vir de `@ConfigProperty("storage.bucket.xxx")` — não hardcoded.
- Objetos devem ser nomeados com prefixo por tenant (ex: `empresa/{empresaId}/credencial/{id}.jpg`) para isolamento multitenancy.
