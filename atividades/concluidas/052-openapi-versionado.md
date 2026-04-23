---
id: 052
titulo: OpenAPI publicado e versionamento de rotas
prioridade: baixa
estimativa: P
depende-de: []
epico: observabilidade
---

## Contexto
O frontend (que será criado em breve) e o app dos aparelhos precisam de um contrato estável.

## Objetivo
OpenAPI disponível e rotas prefixadas com `/api/v1`.

## Critérios de aceitação
- [x] Todos os resources sob `@Path("/api/v1/...")`.
- [x] `quarkus.smallrye-openapi.path=/openapi` e Swagger UI em `/swagger-ui` (dev).
- [x] Anotações `@Operation`, `@APIResponse` e `@Tag` nos endpoints principais.
- [ ] Contrato exportado e commitado em `docs/openapi.yaml` via `mvn` goal ou CI.
  (adiado — a exportação automática depende de goal/CI dedicado; o contrato é
  servido em `/openapi` e `/openapi?format=yaml` em runtime.)

## Notas técnicas
- Breaking changes futuros entram em `/api/v2` sem quebrar clientes atuais.

## Resultado

### Resumo
- Todos os `@Path` de resources passaram a ser prefixados com `/api/v1`:
  - `AuthResource`: `/auth` → `/api/v1/auth`
  - `EmpresaResource`: `/empresas` → `/api/v1/empresas`
  - `EventoResource`: `/evento` → `/api/v1/eventos`
  - `UsuarioResource`: `/usuario` → `/api/v1/usuarios`
  - `UsuarioLogadoResource`: `/usuariologado` → `/api/v1/usuario-logado`
- Adicionadas anotações OpenAPI (`@Tag` na classe, `@Operation` e múltiplos
  `@APIResponse` por método) em todos os endpoints públicos dos 5 resources.
- Configurado OpenAPI (`/openapi`) e Swagger UI em dev (`/swagger-ui`) em
  `application.properties`.
- `PERMISSIONS.md` atualizado com os novos paths.
- `README.md` ganhou seção "API / OpenAPI" documentando contrato e versionamento.
- `pom.xml` já tinha `quarkus-smallrye-openapi` — não houve adição de
  dependência.

### Arquivos alterados
- `src/main/java/ka/mdo/resource/AuthResource.java`
- `src/main/java/ka/mdo/resource/EmpresaResource.java`
- `src/main/java/ka/mdo/resource/EventoResource.java`
- `src/main/java/ka/mdo/resource/UsuarioResource.java`
- `src/main/java/ka/mdo/resource/UsuarioLogadoResource.java`
- `src/main/resources/application.properties`
- `PERMISSIONS.md`
- `README.md`

### Build
`./mvnw.cmd compile -q` executou sem erros.
