---
id: 014
titulo: CRUD de Aparelho
prioridade: alta
estimativa: M
depende-de: [011]
epico: credencial
---

## Contexto
Não existe `AparelhoResource`. Aparelhos hoje só podem ser inseridos direto no
banco — não há como cadastrar/listar/desativar via API. O painel do gestor não
consegue gerenciar os totens.

## Objetivo
CRUD completo de `Aparelho`, isolado por tenant.

## Critérios de aceitação
- [x] `GET /aparelhos` (ADMIN_EMPRESA / SUPER_ADMIN) — lista paginada com
      filtros por `ativo`, `eventoId`, `localEspecificoId`.
- [x] `GET /aparelhos/{id}` — detalhe.
- [x] `POST /aparelhos` — cria com `descricao`, `eventoId` opcional,
      `localEspecificoId` opcional. Empresa vem do JWT.
- [x] `PUT /aparelhos/{id}` — edita descrição e vínculos (evento/local).
- [x] `PATCH /aparelhos/{id}/desativar` e `/reativar` — toggle `ativo`.
- [x] Validação cross-tenant: `eventoId` e `localEspecificoId` devem pertencer
      ao mesmo tenant (403 senão).
- [x] PERMISSIONS.md atualizado.

## Notas técnicas
- Filtro Hibernate `tenantFilter` já cobre `Aparelho`.
- Preparar entidade para 007 (credencial M2M) — campos `clientId`/`secretHash`
  podem ser adicionados em migração separada.

## Resultado

### O que foi feito
CRUD completo de `Aparelho` implementado em `AparelhoResource` +
`AparelhoService`, restrito a `ADMIN_EMPRESA` e `SUPER_ADMIN`. Sem
prefixo `/api/v1` (já removido em commit anterior). Todas as operações
filtram por tenant via `tenantFilter` do Hibernate; a empresa do
aparelho vem sempre do claim `empresaId` do JWT — nunca do payload.

### Endpoints
- `GET /aparelhos` — paginado (`pagina`/`tamanho`, default 0/20) com
  filtros opcionais `ativo`, `eventoId`, `localEspecificoId`.
- `GET /aparelhos/{id}` — detalhe.
- `POST /aparelhos` — cria.
- `PUT /aparelhos/{id}` — edita `descricao` e vínculos.
- `PATCH /aparelhos/{id}/desativar` — UPDATE explícito de `ativo=false`
  (o `prePersist` da `EntityClass` só toca `ativo` na criação).
- `PATCH /aparelhos/{id}/reativar` — UPDATE explícito de `ativo=true`.

### Validação cross-tenant
`eventoId` e `localEspecificoId` são resolvidos via `findById` filtrado
pelo `tenantFilter`. Como `Evento` e `EspacoEvento` também usam o
filtro, um id de outro tenant volta `null` no repositório — o service
responde 403 ("Evento/Espaço pertence a outra empresa ou não existe").
Optei por 403 ao invés de 404 para deixar claro ao painel que o
vínculo cruzou tenant, ainda que ambos os casos (inexistente, outro
tenant) sejam indistinguíveis pelo Hibernate.

### Arquivos
**Novos**
- `src/main/java/ka/mdo/resource/AparelhoResource.java`
- `src/main/java/ka/mdo/service/AparelhoService.java`
- `src/main/java/ka/mdo/dto/AparelhoDTO.java`
- `src/main/java/ka/mdo/dto/AparelhoResponseDTO.java`

**Alterados**
- `src/main/java/ka/mdo/repository/AparelhoRepository.java` — adicionado
  método `listarFiltrado(ativo, eventoId, localEspecificoId, page, size)`
  que monta dinamicamente a HQL com base nos filtros não-nulos e ordena
  por `id DESC`.
- `PERMISSIONS.md` — 6 linhas novas para os endpoints e nota descrevendo
  a validação cross-tenant (id 014).

### Build
`./mvnw -B -ntp -DskipTests package` → BUILD SUCCESS.

### Não tocado
Conforme escopo da atividade: nenhuma migração nova (V6 já tem todas as
colunas necessárias), nenhuma mudança em `Aparelho.java`, `AcessoService`,
`AcessoResource` ou em outros resources. Sem testes novos (`src/test/**`
fora do escopo).
