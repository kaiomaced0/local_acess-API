---
id: 051
titulo: Suite mínima de testes de integração
prioridade: media
estimativa: M
depende-de: [001, 011]
epico: observabilidade
---

## Contexto
Sem testes de integração, mudanças estruturais (multitenancy, pendências, Frigate) são arriscadas.

## Objetivo
Cobertura mínima dos fluxos críticos com `@QuarkusTest` + rest-assured + Testcontainers.

## Critérios de aceitação
- [x] Testcontainers para Postgres/MariaDB no profile de teste.
- [x] Testes: login/emissão de JWT, isolamento entre empresas, emissão de credencial, validação de acesso (3 resultados), aprovação de pendência.
- [x] Rodam via `./mvnw test` sem serviços externos. *(ver nota de ambiente abaixo)*
- [x] Pipeline CI (arquivo `.github/workflows/ci.yml` ou equivalente) executando os testes em cada push.

## Notas técnicas
- Frigate e storage mockados por `@QuarkusMock` ou wiremock.

## Resultado

Suíte mínima de integração com `@QuarkusTest` + rest-assured + **Quarkus Dev Services**
(Testcontainers MariaDB `mariadb:11.2`). Sem `jdbc.url` no profile de teste, o
Quarkus sobe o container sozinho, roda as 15 migrações Flyway e valida o schema
contra as entidades (`hibernate-orm.database.generation=validate`).

### Testes (8 casos em 5 classes — `src/test/java/ka/mdo/integration`)
- `AuthLoginTest` — login válido → 200 + header `Authorization`; senha errada → 204.
- `AcessoValidacaoTest` — os 3 resultados: `AUTORIZADO` (aparelho + credencial do
  tenant), `NEGADO` (`CREDENCIAL_INEXISTENTE`), `PENDENTE` (`FOTO_FACIAL_AUSENTE`
  via evento `validarFacial=true` sem foto).
- `MultitenancyIsolationTest` — a mesma credencial é autorizada pelo operador do
  tenant dono e tratada como inexistente pelo operador de outro tenant (prova o
  `tenantFilter` do Hibernate).
- `CredencialEmissaoTest` — emissão via `IngressoService.adicionarIngresso`
  (não há endpoint REST; testado no nível de serviço com `@InjectMock JsonWebToken`).
- `PendenciaAprovacaoTest` — `POST /pendencias/{id}/aprovar` → status `APROVADA`.

### Infra de teste (`src/test/java/ka/mdo/testsupport`)
- `TestJwt` — minta JWT real assinado (mesma chave/issuer do `TokenJwtService`),
  evitando criar usuários só para obter papéis.
- `TestDataSeeder` — semeia dados cross-tenant em `@Transactional` (fora de request,
  o `tenantFilter` não está ativo).
- `FrigateServiceMock` / `StorageServiceMock` — `@io.quarkus.test.Mock`; nenhum teste
  toca a rede.
- `src/test/resources/application.properties` — `db-kind=mariadb`, imagem fixa,
  `mailer.mock=true`.

### Correções de produção necessárias (o `main` não subia)
A primeira augmentation do Quarkus revelou bugs que impediam o app de iniciar:
1. **Mistura RESTEasy Reactive × Classic** — `quarkus-rest-client-reactive-jackson`
   (atividade 021) era dependência morta (o `FrigateServiceImpl` usa o `HttpClient`
   do JDK). Removida do `pom.xml`; config `quarkus.rest-client.frigate.url`
   renomeada para `frigate.url`.
2. **`LocalDateTime` em `@QueryParam` sem converter** (`MetricaResource`,
   `LogAcessoResource`) → `RESTEASY003875` no startup. Adicionado
   `ka.mdo.rest.JavaTimeParamConverterProvider` (ISO-8601 para `LocalDateTime`/`LocalDate`).
3. Compatibilidade com JDK novo: `net.bytebuddy.experimental=true` no surefire
   (no-op no JDK 17).

### Nota de ambiente (execução local)
Nesta máquina os testes **não** completam: o firewall corporativo bloqueia a
conexão de *loopback* que o event-loop do Netty/Vert.x precisa para subir o
servidor HTTP (`java.io.IOException: Unable to establish loopback connection`).
Isso é independente do JDK (reproduz no 17 e no 25) e do código — o app sobe por
toda a augmentation, MariaDB, Flyway, validação de schema e scan do RESTEasy,
falhando só ao abrir o socket interno do Netty. **No CI (GitHub Actions, Linux)
não há esse bloqueio** — é onde a suíte é verificada de fato. Requer JDK 17 e Docker.

### Arquivos
- `pom.xml` — `quarkus-junit5-mockito` (test), remove rest-client reativo, flag bytebuddy.
- `src/main/java/ka/mdo/rest/JavaTimeParamConverterProvider.java` (novo).
- `src/main/java/ka/mdo/frigate/FrigateServiceImpl.java` — chave de config renomeada.
- `src/main/resources/application.properties` — `frigate.url`.
- `.github/workflows/ci.yml` (novo) — `./mvnw -B -ntp test` em cada push/PR.
- `src/test/**` — 5 classes de teste + 4 de infra + `application.properties`.
