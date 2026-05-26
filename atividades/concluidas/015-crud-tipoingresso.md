---
id: 015
titulo: CRUD de TipoIngresso
prioridade: alta
estimativa: P
depende-de: [001, 002, 003]
epico: credencial
---

## Contexto
A entidade `TipoIngresso` já existe (campo `nome`, N:N com `EspacoEvento`,
multi-tenant via `empresa_id`) e é referenciada por `Ingresso` e pelas
autorizações por local (atividade 030). Hoje, porém, não há endpoints REST
para gerenciá-la — o cadastro só é viável via SQL ou via outros fluxos
indiretos. Sem CRUD, gestores não conseguem evoluir o catálogo de tipos de
ingresso da empresa.

## Objetivo
Expor um CRUD básico de `TipoIngresso` em `/tipos-ingresso`, isolado por
tenant, com validações de unicidade do nome por empresa e proteção
contra exclusão de tipos que ainda têm credenciais ativas.

## Critérios de aceitação
- [x] `GET /tipos-ingresso` — lista os tipos de ingresso ativos do tenant.
      Suporta `?incluirInativos=true`. Acesso: `ADMIN_EMPRESA`,
      `GESTOR_EVENTO`, `SUPER_ADMIN`.
- [x] `GET /tipos-ingresso/{id}` — retorna um tipo de ingresso por id, do
      tenant atual.
- [x] `POST /tipos-ingresso` — cria um novo tipo de ingresso com `nome`
      (NotBlank, max 100). Falha 409 se já existir tipo ativo com o mesmo
      nome na empresa.
- [x] `PUT /tipos-ingresso/{id}` — edita o `nome` (mesma validação de
      unicidade).
- [x] `DELETE /tipos-ingresso/{id}` — **soft-delete** (`ativo=false`).
      **Falha 409 Conflict se existir `Ingresso` ativo referenciando esse
      tipo.**
- [x] Multitenancy: todas as operações filtram por `empresaId` do JWT;
      acesso cross-tenant é 403.
- [x] Migração `V16__tipoingresso_unique_empresa_nome.sql` adiciona o
      índice único `(empresa_id, nome)`.
- [x] `PERMISSIONS.md` atualizado com os novos endpoints.

## Notas técnicas
- A entidade `TipoIngresso` mantém-se intocada — já tem `@Filter(tenantFilter)`
  e FK `empresa_id NOT NULL`.
- DTO request `TipoIngressoDTO`: apenas `nome` (NotBlank, Size max=100).
- Response `TipoIngressoResponseDTO`: `id`, `nome`, `ativo`.
- Pré-validação de unicidade no `service` (busca por nome ativo) +
  constraint no banco como rede de segurança contra concorrência.
- Validação anti-delete: `IngressoRepository.count("tipoIngresso.id = ?1 AND ativo = true", id) > 0`
  → `WebApplicationException(409)`.
- `IngressoResource` ainda usa prefixo `/api/v1`, mas o novo CRUD entra
  sem prefixo, conforme convenção adotada após o commit `4e946d0`.

## Resultado

### Arquivos criados
- `src/main/java/ka/mdo/dto/TipoIngressoDTO.java` — request com `nome`
  (`@NotBlank @Size(max=100)`).
- `src/main/java/ka/mdo/dto/TipoIngressoResponseDTO.java` — response
  enxuto (`id`, `nome`, `ativo`).
- `src/main/java/ka/mdo/service/TipoIngressoService.java` — regras de
  negócio: `empresaDoJwt()` + `validarMesmoTenant()` (com no-op para
  SUPER_ADMIN cross-tenant), pré-validação de unicidade do nome,
  soft-delete bloqueado quando há credenciais ativas.
- `src/main/java/ka/mdo/resource/TipoIngressoResource.java` —
  `GET/POST/PUT/DELETE /tipos-ingresso` (sem prefixo `/api/v1`).
  `@RolesAllowed({"ADMIN_EMPRESA","GESTOR_EVENTO","SUPER_ADMIN"})`.
- `src/main/resources/db/migration/V16__tipoingresso_unique_empresa_nome.sql`
  — `CREATE UNIQUE INDEX uk_tipoingresso_empresa_nome ON TipoIngresso (empresa_id, nome)`.

### Arquivos alterados
- `src/main/java/ka/mdo/repository/TipoIngressoRepository.java` — adiciona
  `findAtivoByNomeExato(String, Long)` (com excluirId opcional para
  update) e `listarDoTenant(boolean, int, int)` (paginação Panache com
  filtro de `ativo`). Mantém `findByNome` original.
- `PERMISSIONS.md` — adiciona 5 linhas na tabela de endpoints e uma nota
  de atividade 015 descrevendo as decisões.

### Decisões de implementação

**Sem prefixo `/api/v1`**: o commit `4e946d0` na linha histórica mais
recente removeu o versionamento das rotas. Como este branch parte de
`3e9e50a` (anterior à remoção), o novo recurso entra já na convenção
nova (`/tipos-ingresso`). Resources antigos continuam em `/api/v1/*`
até serem migrados.

**Unicidade do nome — dupla barreira**:
1. Pré-validação no service via `repository.findAtivoByNomeExato(nome, excluirId)`
   — retorna 409 amigável.
2. Constraint `UNIQUE (empresa_id, nome)` no banco — rede de segurança
   contra duas requisições POST concorrentes que passam ambas na
   pré-validação. A constraint cobre ativos+inativos (sem
   partial index, porque MariaDB não suporta — manter portabilidade
   da migração).

**Soft-delete com guarda contra órfãos**: `DELETE` faz
`ingressoRepository.count("tipoIngresso.id = ?1 AND ativo = true", id)`.
Se > 0, lança `WebApplicationException(409)` com mensagem detalhada
("Não é possível excluir: existem N credenciais ativas..."). Tipos com
apenas credenciais inativas podem ser desativados — assume-se que a
auditoria já fez seu papel sobre as credenciais inativas.

**Multitenancy**: aproveita `@Filter(tenantFilter)` da entidade para
leitura; em escritas, o service preenche `empresa` a partir do JWT
(`empresaDoJwt()`). Em leituras por id, faz `validarMesmoTenant()` —
no-op se JWT é SUPER_ADMIN sem `empresaId` (cross-tenant). Padrão
copiado do `EventoService`.

**Paginação**: query params `pagina` (default 0) e `tamanho`
(default 20, máx 200), usando `io.quarkus.panache.common.Page.of(...)`.
Ordenação fixa por `id` ascendente para resultados estáveis entre
páginas. Mesmo padrão do `NotificacaoRepository`.

### Build status
- `./mvnw -B -ntp -DskipTests compile` → **BUILD SUCCESS** (136 fontes
  compilando sem erros nem warnings, incluindo os 4 novos arquivos
  criados aqui).
- `./mvnw -B -ntp -DskipTests package` → **falha pré-existente** no
  passo `quarkus:3.5.3:build` com erro
  `Mixing RESTEasy Reactive and RESTEasy Classic server parts is not supported`.
  O pom traz `quarkus-resteasy` (classic) + `quarkus-rest-client-reactive-jackson`
  (reactive) — conflito não introduzido por esta atividade (reproduz em
  `main` limpo: `git stash && ./mvnw package` falha do mesmo modo).
  O fix vive no branch `chore/limpeza-tmp-e-testes-integracao` (remove
  `quarkus-rest-client-reactive-jackson`), mas o escopo desta atividade
  proíbe alterar `pom.xml`. Quando esse branch for mergeado em `main`
  o build do package volta a passar — nenhuma mudança aqui é
  necessária no momento.

### Checklist
- ✅ Entidade `TipoIngresso` não foi tocada (conforme instrução).
- ✅ DTOs request/response criados.
- ✅ Repository ganhou queries auxiliares sem perder a query original.
- ✅ Service isola por tenant + valida unicidade + bloqueia delete com
  credenciais ativas.
- ✅ Resource com `@RolesAllowed`, `@Valid` no payload, OpenAPI tags e
  códigos de resposta documentados.
- ✅ Migração V16 portável SQL (CREATE UNIQUE INDEX padrão).
- ✅ `PERMISSIONS.md` atualizado (5 endpoints + nota).
- ✅ Compile passa.
- ⚠️  Package (Quarkus build) falha por conflito pré-existente em
  `pom.xml` — fora do escopo desta atividade (ver "Build status").
- ❌ Testes automatizados — registrados como débito para 051.

