---
id: 050
titulo: Logs estruturados (JSON) com correlação por request-id
prioridade: baixa
estimativa: P
depende-de: []
epico: observabilidade
---

## Contexto
Debug em produção exige logs correlacionáveis entre request, service e integrações externas (Frigate, storage).

## Objetivo
Log JSON com MDC contendo requestId, empresaId, usuarioId.

## Critérios de aceitação
- [x] `quarkus.log.console.json=true` em produção.
- [x] Filtro JAX-RS gera `requestId` (ou usa header `X-Request-Id` quando presente) e popula MDC.
- [x] Claims `empresaId` e `usuarioId` do JWT também no MDC.
- [x] Log nunca inclui senha, token da credencial ou documento pessoal.

## Notas técnicas
- `org.jboss.logging.MDC` funciona com Quarkus.

## Resultado

### Implementação
- Dependência `quarkus-logging-json` adicionada ao `pom.xml` (única adição; demais dependências intactas).
- `application.properties`: adicionadas apenas propriedades `quarkus.log.*` (JSON em `%prod`, console formatado em `%dev`, formato com MDC `[requestId,empresaId,usuarioId]`).
- Novo filtro JAX-RS `LoggingRequestFilter` (`@Priority(Priorities.AUTHENTICATION - 100)`) que roda antes do `TenantRequestFilter`:
  - Lê header `X-Request-Id`; gera UUID quando ausente.
  - Popula MDC com `requestId`, `empresaId` e `usuarioId` (sub do JWT) quando o token existe.
  - No response, devolve header `X-Request-Id` e limpa MDC para evitar vazamento entre requisições na mesma thread.
- Novo util `LogSanitizer.mascarar(String)` — disponível para futuros loggers; string > 20 chars alfanumérica e sem espaço vira `***`.

### Revisão de logs existentes
Varredura em `src/main/java` por `Log.*` e `System.out` confirmou que nenhum logger em produção imprime senha, token de credencial ou CPF. O único `System.out` está em `HashService.main(...)` (método `main` de desenvolvimento, não executa em runtime) — deixado intacto conforme orientação "só corrija vazamentos óbvios".

### Arquivos
- `pom.xml` (adicionada dependência `quarkus-logging-json`)
- `src/main/resources/application.properties` (bloco de logs estruturados)
- `src/main/java/ka/mdo/logging/LoggingRequestFilter.java` (novo)
- `src/main/java/ka/mdo/logging/LogSanitizer.java` (novo)

### Build
`./mvnw.cmd clean compile` → BUILD SUCCESS (51 arquivos fonte compilados).
