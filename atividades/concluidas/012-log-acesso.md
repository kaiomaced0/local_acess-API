---
id: 012
titulo: Log de acessos para auditoria
prioridade: media
estimativa: P
depende-de: [011]
epico: credencial
---

## Contexto
Precisamos rastrear toda tentativa de acesso para auditoria, dashboards e resolução de disputas.

## Objetivo
Entidade `LogAcesso` registrando cada leitura de credencial.

## Critérios de aceitação
- [x] Entidade com: id, empresaId, credencialId, localId, aparelhoId, resultado, motivo, dataHora, fotoCapturadaUrl (nullable).
- [x] Gravação assíncrona (não bloquear o endpoint de validação).
- [x] Endpoint `GET /logs-acesso` com filtros (credencial, local, data) restrito a gestores.
- [x] Índices em `(empresaId, dataHora)` e `(credencialId, dataHora)`.

## Notas técnicas
- Avaliar tabela particionada por mês se volume for alto.

## Resultado

### Arquivos criados
- `src/main/java/ka/mdo/model/LogAcesso.java` — entidade multi-tenant (`@Filter tenantFilter`), `resultado` STRING, `motivo` 100, `dataHora` NOT NULL, `fotoCapturadaUrl` 500 (placeholder 021). FKs para `Empresa`, `Ingresso` (nullable), `EspacoEvento` (nullable) e `Aparelho` (NOT NULL).
- `src/main/java/ka/mdo/repository/LogAcessoRepository.java` — `PanacheRepository` + método `buscar(...)` que monta query dinamicamente (filtros opcionais) com `Sort.by("dataHora").descending()` e paginação Panache.
- `src/main/java/ka/mdo/dto/LogAcessoResponseDTO.java` — record sem token nem dados pessoais (inclui `aparelhoDescricao` por conveniência do operador).
- `src/main/java/ka/mdo/service/AcessoOcorrido.java` — record imutável transportado entre `AcessoService` e o listener (IDs por valor; sem token).
- `src/main/java/ka/mdo/service/LogAcessoService.java` — listener `@ObservesAsync` + `@Transactional(REQUIRES_NEW)` para persistir o log; método `buscar(...)` para o resource.
- `src/main/java/ka/mdo/resource/LogAcessoResource.java` — `GET /logs-acesso` com `@RolesAllowed({"ADMIN_EMPRESA","GESTOR_EVENTO","GESTOR_LOCAL","SUPER_ADMIN"})`, query params `credencialId`, `localId`, `de`, `ate`, `pagina`, `tamanho`.
- `src/main/resources/db/migration/V7__log_acesso.sql` — tabela `LogAcesso` + índices `idx_log_acesso_empresa_data` e `idx_log_acesso_credencial_data`.

### Arquivos alterados
- `src/main/java/ka/mdo/service/AcessoService.java` — injetado `Event<AcessoOcorrido>`; `negar(...)` refatorado para receber `Aparelho`, `credencialId` e `localId`; dois `TODO 012` substituídos por chamada a `dispararLog(...)`. Caso `APARELHO_INEXISTENTE` não persiste log (sem FK possível) — documentado em comentário.
- `PERMISSIONS.md` — adicionada linha `GET /logs-acesso` (SA/AE/GE/GL ✅, OA/CL ❌).

### Build status
`./mvnw.cmd compile -q` — **OK** (exit 0; apenas warnings do runtime Java 17 sobre `sun.misc.Unsafe` em dependências do Maven).

### Checklist
- ✅ Entidade `LogAcesso` com todos os campos solicitados (+ `ativo`/`dataInclusao` herdados de `EntityClass`).
- ✅ Gravação assíncrona (fire-and-forget via CDI `Event#fireAsync`).
- ✅ Endpoint `GET /logs-acesso` com filtros (credencial, local, data) e roles corretas.
- ✅ Índices `(empresa_id, dataHora)` e `(ingresso_id, dataHora)`.
- ✅ Falha no log NÃO bloqueia o happy path (try/catch em `dispararLog` + `REQUIRES_NEW` no listener).
- ✅ Token nunca é logado (evento só carrega `ingressoId`).
- ✅ TODO futuro de particionamento por mês documentado (migração + Javadoc), não implementado.
- ✅ `fotoCapturadaUrl` existe mas não integra com storage (débito da 021).
- ✅ `TODO 030/031` para restringir `GESTOR_LOCAL` aos seus locais, documentado no resource.
- ❌ Testes de integração — débito consolidado na atividade 051.

### Estratégia de assincronismo
Escolhido **CDI `Event<AcessoOcorrido>` + `@ObservesAsync`** (conforme sugerido no enunciado) em detrimento de `ManagedExecutor`:
1. **Desacoplamento**: quem dispara só conhece o evento (POJO/record), não o serviço que persiste — evita dependência circular `AcessoService ↔ LogAcessoService`.
2. **Transação própria**: `@Transactional(REQUIRES_NEW)` no observer garante que um rollback no request principal NÃO arrasta o log, e um erro no log NÃO propaga para o chamador (o handler roda em outra thread e já encerrou de qualquer forma quando o aparelho recebeu a resposta).
3. **Zero dependência nova**: `jakarta.enterprise.event.Event` já está no classpath do Quarkus ARC.
4. **Resiliente**: try/catch no `AcessoService#dispararLog` cobre falhas do próprio `fireAsync`; try/catch no listener cobre falhas de persistência. Em nenhum caso a resposta ao aparelho é bloqueada.

### TODOs do `AcessoService` fechados
- `TODO 012` no happy path (após `LOG.infof("Acesso AUTORIZADO ...")`) — substituído por `dispararLog(aparelho, ingresso.getId(), req.localId(), AUTORIZADO, MOTIVO_OK)`.
- `TODO 012` em `negar(...)` — método foi reassinado para `negar(Aparelho, Long credencialId, Long localId, String motivo)` e agora chama `dispararLog` internamente antes de devolver o DTO. Todos os cinco pontos de `negar` (aparelho inativo, credencial inexistente, evento fora do período, local inexistente, local de outro evento) passaram a gravar log. Exceção: `APARELHO_INEXISTENTE` é devolvido sem log porque `aparelho_id` é NOT NULL em `LogAcesso` — documentado em comentário inline.

### Decisões adicionais
- **Paginação** default `pagina=0`, `tamanho=20`, teto de `200` para evitar dumps de auditoria em produção.
- **Ordenação** fixa `dataHora DESC` — auditoria cresce monotonicamente e o caso de uso é "mostre os últimos N".
- **`GESTOR_LOCAL` temporariamente vê tudo do tenant** — `// TODO 030/031` visível no resource até que o vínculo usuário→locais exista.
