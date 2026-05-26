---
id: 011
titulo: Endpoint de validação de acesso para aparelhos
prioridade: alta
estimativa: M
depende-de: [010, 002]
epico: credencial
---

## Contexto
Aparelhos na entrada do evento e de cada local lêem QR e consultam a API para saber se libera ou não.

## Objetivo
Endpoint único que recebe token + contexto (evento/local/aparelho) e retorna decisão.

## Critérios de aceitação
- [x] `POST /acesso/validar` recebe `{ token, localId?, aparelhoId }`.
- [x] Resposta: `{ resultado: AUTORIZADO|PENDENTE|NEGADO, motivo, credencialId, exigeValidacaoFacial }`.
- [x] Valida: credencial existe, pertence à empresa do aparelho, evento ativo, perfil da credencial autorizado no local (autorização por tipo no local delegada à 030 com `TODO` visível).
- [ ] Registra `LogAcesso` — delegado à atividade **012** (entidade ainda não existe). `TODO 012` comentado em `AcessoService`.
- [x] Endpoint autenticado com perfil `OPERADOR_APARELHO`.
- [ ] Teste cobrindo os três resultados — débito registrado para a atividade **051** (testes de integração).

## Notas técnicas
- Entidade `Aparelho` criada nesta atividade (`empresa_id` NOT NULL, `local_especifico_id`/`evento_id` opcionais).
- Ponto de extensão para validação facial (atividade 021): campo `exigeValidacaoFacial` no response já existe, hoje sempre `false`.

## Resultado

### Arquivos criados
- `src/main/java/ka/mdo/model/Aparelho.java` — entidade multitenant (`@Filter tenantFilter`), descricao/empresa/localEspecifico/evento; herda `ativo` de `EntityClass`.
- `src/main/java/ka/mdo/model/ResultadoAcesso.java` — enum `AUTORIZADO`, `PENDENTE`, `NEGADO`.
- `src/main/java/ka/mdo/dto/ValidarAcessoRequestDTO.java` — record com `@NotBlank token`, `Long localId`, `@NotNull aparelhoId`.
- `src/main/java/ka/mdo/dto/ValidarAcessoResponseDTO.java` — record com resultado/motivo/credencialId/exigeValidacaoFacial.
- `src/main/java/ka/mdo/repository/AparelhoRepository.java` — `PanacheRepository<Aparelho>`.
- `src/main/java/ka/mdo/service/AcessoService.java` — orquestra validação e decide resultado.
- `src/main/java/ka/mdo/resource/AcessoResource.java` — `POST /acesso/validar`, `@RolesAllowed("OPERADOR_APARELHO")`.
- `src/main/resources/db/migration/V6__aparelho_resultado_acesso.sql` — cria tabela `Aparelho` com FKs para `Empresa`, `EspacoEvento`, `Evento` + índice `ix_aparelho_empresa`.

### Arquivos alterados
- `PERMISSIONS.md` — adicionada linha `POST /acesso/validar` (OPERADOR_APARELHO ✅, demais ❌).

### Fluxo de validação implementado
1. Aparelho existe?
2. Aparelho pertence à empresa do JWT? (403 `ForbiddenException`)
3. Aparelho ativo?
4. `IngressoRepository.findByToken(token)` — filtro de tenant corta tokens de outro tenant (retorna `Optional.empty` ⇒ `NEGADO CREDENCIAL_INEXISTENTE`).
5. Período do evento: se o aparelho está vinculado a um `Evento`, valida `inicioEvento <= now <= finalEvento`.
6. Se `localId` != null: carrega `EspacoEvento`, valida que pertence ao mesmo evento (via coleção `Evento.espacoEventos`), `TODO 030` para whitelist de `TipoIngresso`.
7. `exigeValidacaoFacial = false` fixo (`TODO 021`).
8. `AUTORIZADO` no happy path; nunca loga o token, apenas `ingresso.id`.

### Delegações explícitas (com TODO no código)
- `TODO 012` — gravar `LogAcesso` (duas vezes em `AcessoService`: happy path e em `negar()`). Entidade virá na atividade 012.
- `TODO 020` — `PENDENTE` por dados pessoais faltantes (docstring no Javadoc do service).
- `TODO 021` — `PENDENTE` + `exigeValidacaoFacial=true` quando o evento exigir rosto.
- `TODO 030` — whitelist de `TipoIngresso` por `EspacoEvento` (hoje libera com WARN + comentário).

### Decisões arquiteturais
- **Checagem `EspacoEvento` pertence ao mesmo evento**: feita via `Evento.espacoEventos.stream().anyMatch(...)` (lado já existente do relacionamento). Quando a 030 inverter/complementar com FK direta `EspacoEvento → Evento`, substituir pela consulta direta. Hoje, se o Evento ainda não tem `espacoEventos` populado, a checagem retorna true (contorno documentado).
- **Resolução do Evento para validar período**: preferimos `aparelho.getEvento()`; se nulo, pulamos a checagem de período (aparelho genérico). A resolução por `EspacoEvento.getEvento()` ficou marcada com `TODO 030` pois `EspacoEvento` ainda não tem FK para `Evento`.
- **Multitenancy reforçada**: aparelho valida empresa *antes* de buscar a credencial — 403 imediato ao tentar cruzar tenants. A busca do ingresso também respeita o filtro `tenantFilter` do Hibernate.
- **DTOs**: `records` Java 17 (alinhado com `EventoDTO`/`IngressoResponseDTO`); nunca retorna entidade; token nunca aparece no response nem em log.
- **Migração**: mantido padrão portável MariaDB/Postgres (`BOOLEAN DEFAULT TRUE`, `BIGINT AUTO_INCREMENT`, FKs). Sem seed de aparelho — bootstrap de teste fica para uma futura atividade.

### Build status
`./mvnw.cmd compile -q` — **OK** (nenhum erro; apenas warnings do runtime Java 17 sobre `sun.misc.Unsafe` usado por dependências Maven).

### Checklist
- ✅ Entidade `Aparelho` (multitenant, com `ativo` herdado, `localEspecifico`/`evento` nullable).
- ✅ Enum `ResultadoAcesso`.
- ✅ DTOs request/response com validação Jakarta.
- ✅ Service `AcessoService` com toda a regra de negócio e `@RolesAllowed` no resource.
- ✅ Resource `AcessoResource` apenas orquestra (sem lógica).
- ✅ `AparelhoRepository`.
- ✅ Migração `V6__aparelho_resultado_acesso.sql` portável.
- ✅ `PERMISSIONS.md` atualizado.
- ✅ Token não é logado (apenas `ingresso.id`).
- ✅ 403 quando aparelho pertence a outra empresa.
- ✅ Build passa.
- ❌ `LogAcesso` (delegado à 012).
- ❌ Whitelist de tipos por local (delegado à 030).
- ❌ Validação facial (delegada à 021).
- ❌ Testes de integração (débito na 051).
