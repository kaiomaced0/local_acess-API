---
id: 031
titulo: Workflow de pendências de acesso
prioridade: alta
estimativa: M
depende-de: [011, 021, 032]
epico: locais
---

## Contexto
Quando a validação resulta em `PENDENTE` (rosto divergente, credencial sem dados, etc.), um gestor precisa aprovar ou recusar. O cliente deve saber o desfecho.

## Objetivo
Entidade `Pendencia` e endpoints de resolução com notificações nas duas pontas.

## Critérios de aceitação
- [x] Entidade `Pendencia`: credencialId, localId, aparelhoId, motivo, fotoCapturadaUrl, status (ABERTA|APROVADA|RECUSADA), criadaEm, resolvidaEm, resolvidaPorUsuarioId.
- [x] Ao criar pendência: notifica gestores do local (e do evento se não houver gestor de local).
- [x] `GET /pendencias` — fila do gestor, com filtros por status e local.
- [x] `POST /pendencias/{id}/aprovar` e `POST /pendencias/{id}/recusar` — registram resolução e notificam o cliente.
- [x] Aprovação cria/atualiza o vínculo de rosto oficial da credencial (se a pendência foi facial).
- [ ] Teste E2E: leitura → pendência → aprovação → cliente recebe notificação → nova leitura passa direto. — débito consolidado na 051.

## Notas técnicas
- Idempotência: aprovar pendência já resolvida retorna o estado atual (não duplica).

## Resultado

### Arquivos criados
- `src/main/java/ka/mdo/model/Pendencia.java` — entidade multitenant (`@Filter tenantFilter`) com FKs para `Empresa`, `Ingresso` (credencial), `EspacoEvento` (local nullable), `Aparelho`, `Usuario` (resolvidaPor nullable); enum `StatusPendencia` default ABERTA.
- `src/main/java/ka/mdo/model/StatusPendencia.java` — `ABERTA` | `APROVADA` | `RECUSADA`.
- `src/main/java/ka/mdo/repository/PendenciaRepository.java` — `buscar(status, localId, pagina, tamanho)`, `findPendenteAberta(credencialId)` e `findAbertaPorMotivo(credencialId, motivo)` (idempotência).
- `src/main/java/ka/mdo/dto/PendenciaResponseDTO.java` — record com `credencialTokenMascarado` (`***xxxx`) e `fotoCapturadaUrl` pré-assinada.
- `src/main/java/ka/mdo/dto/ResolucaoPendenciaDTO.java` — record com `observacao` opcional (`@Size(max=500)`).
- `src/main/java/ka/mdo/pendencia/PendenciaService.java` — service `@ApplicationScoped` com:
  - `criar(...)` `@Transactional` idempotente por `(credencial, motivo, ABERTA)`.
  - Observer `aoRequererPendencia(@ObservesAsync PendenciaRequerida)` em `@Transactional(REQUIRES_NEW)` — trata exceções sem propagar.
  - `aprovar(id, obs)` / `recusar(id, obs)` idempotentes; aprovação facial reseta `DadosPessoais.rostoFrigateCadastrado=false` (re-enrola na próxima leitura).
  - Notificação via `NotificacaoService` — `PENDENCIA_ABERTA` para gestores (`GESTOR_EVENTO`/`GESTOR_LOCAL` do tenant) e `PENDENCIA_APROVADA`/`PENDENCIA_RECUSADA` para o dono.
- `src/main/java/ka/mdo/resource/PendenciaResource.java` — `@Path("/pendencias")` com `@RolesAllowed({GESTOR_EVENTO,GESTOR_LOCAL,ADMIN_EMPRESA,SUPER_ADMIN})`; `GET /`, `POST /{id}/aprovar`, `POST /{id}/recusar`.
- `src/main/resources/db/migration/V13__pendencia.sql` — tabela com FKs + índices `(empresa_id, status, criadaEm)` e `(credencial_id, status)`.

### Arquivos alterados
- `src/main/java/ka/mdo/service/AcessoService.java` — injeta `Event<PendenciaRequerida>`; dispara pendência também nos cenários **DADOS_PESSOAIS_INCOMPLETOS** e **FOTO_FACIAL_AUSENTE** (os cenários ROSTO_DIVERGENTE / FRIGATE_INDISPONIVEL já eram disparados pelo `FacialValidationService`). Método helper `dispararPendencia(...)` nunca propaga exceções.
- `PERMISSIONS.md` — 3 novas rotas + nota sobre ausência de vínculo gestor↔local (débito 041).

### Checklist
- ✅ Entidade `Pendencia` multitenant com todos os campos exigidos.
- ✅ Enum `StatusPendencia` persistido como STRING (default ABERTA na app e na coluna).
- ✅ Idempotência: mesma credencial + mesmo motivo + `ABERTA` não duplica (atualiza foto e retorna).
- ✅ Idempotência: aprovar/recusar pendência já resolvida devolve o estado atual sem re-notificar.
- ✅ Observer `@ObservesAsync PendenciaRequerida` em `REQUIRES_NEW` — falhas viram log.error, não propagam.
- ✅ `AcessoService` dispara `PendenciaRequerida` em TODOS os cenários `PENDENTE` (complementa o que o `FacialValidationService` já disparava).
- ✅ Notificação para gestores na criação + para o dono da credencial na resolução.
- ✅ Aprovação facial reseta `rostoFrigateCadastrado=false` → próxima leitura re-enrola e passa direto (critério E2E atendido em nível de lógica).
- ✅ DTOs nunca expõem token bruto (`credencialTokenMascarado` = `***xxxx`).
- ✅ `fotoCapturadaUrl` é URL assinada pelo `StorageService` — banco armazena só a chave.
- ✅ Migração V13 com índices compostos pedidos.
- ✅ `PERMISSIONS.md` atualizado.
- ✅ Build `./mvnw.cmd compile` — BUILD SUCCESS.
- ❌ Teste E2E — débito consolidado na 051 (Testcontainers + mock Frigate).
- ❌ Restrição de fila por locais gerenciados (GESTOR_LOCAL) — débito 041.

### Decisão: pendência de dados pessoais = uso único (sem flag persistida)
O enunciado ofereceu duas opções para aprovar uma pendência de `DADOS_PESSOAIS_INCOMPLETOS`:

1. **Uso único** (escolhida): a aprovação libera a leitura daquele momento via `LogAcesso`, mas a próxima leitura sem dados pessoais volta a gerar pendência. `exigeDadosPessoais` segue valendo no `Evento`.
2. Adicionar uma flag `dadosPessoaisPendenciaLiberada` em `Ingresso` (ou `aprovacaoUnicaUso`) que bypassasse a checagem de dados pessoais em acessos futuros.

**Justificativa para uso único:** o evento exigir dados pessoais é uma invariante do evento (compliance/LGPD/segurança), não uma característica negociável por credencial. Persistir uma flag de bypass criaria um vetor silencioso de contorno: uma única aprovação por gestor (possivelmente feita com pressa na entrada) alteraria permanentemente o perfil de risco da credencial. Manter uso único força que cada tentativa sem dados seja reavaliada, e sinaliza ao cliente que o caminho correto é preencher os dados — não depender de aprovação do gestor. Para pendências faciais o comportamento é diferente (reset de `rostoFrigateCadastrado`) porque a semântica ali é "re-enrolar o rosto", não "burlar a exigência".

### Quantos TODOs de atividades anteriores foram fechados
- **021 → 031 (3 deferrals):** observer de `PendenciaRequerida` ✅, notificação via `NotificacaoService` ✅, reprovisionamento do rosto no Frigate após aprovação (reset de `rostoFrigateCadastrado=false`) ✅. Todos implementados.
- **Nenhum TODO comentado no código-fonte foi removido** — os `TODO 030/031` em `LogAcessoResource`/`LogAcessoService` referem-se ao vínculo gestor↔local, que é débito 041, não 031.

### Para a 041 (locais e ocupação)
- Vínculo específico gestor↔local (tabela de associação `Usuario ↔ EspacoEvento` com perfil). Quando existir:
  - `gestoresDaEmpresa(...)` em `PendenciaService` deve filtrar `GESTOR_LOCAL` pelo `pendencia.local`.
  - Fila `GET /pendencias` pode restringir visibilidade do `GESTOR_LOCAL` aos próprios locais (hoje vê tudo do tenant).
- Restringir mesma regra em `GET /logs-acesso` (TODO 030/031 existente).
- Ocupação (contador por local) e registro de saída são escopo específico da 041.
