---
id: 041
titulo: Endpoints de métricas e dashboard do gestor
prioridade: baixa
estimativa: M
depende-de: [012, 031]
epico: mapa
---

## Contexto
Gestores precisam enxergar em tempo real: quantas pessoas entraram em cada local, pendências abertas, fluxo ao longo do tempo.

## Objetivo
Endpoints agregados para alimentar o painel.

## Critérios de aceitação
- [x] `GET /metricas/evento/{id}/ocupacao` — pessoas atualmente dentro de cada local.
- [x] `GET /metricas/evento/{id}/entradas?granularidade=hora` — série temporal.
- [x] `GET /metricas/evento/{id}/pendencias` — contagem por status e local.
- [x] Respostas com cache curto (30s) para reduzir carga.
- [x] Restrito a gestor do evento/local.

## Notas técnicas
- Ocupação exige modelagem de saída (hoje só há entrada) — adicionar tipo de evento `ENTRADA|SAIDA` em `LogAcesso` ou atividade dedicada.

## Resultado

### Arquivos criados
- `src/main/java/ka/mdo/model/TipoMovimento.java` — enum `ENTRADA|SAIDA`.
- `src/main/java/ka/mdo/model/GestorLocal.java` — vínculo N:N multitenant (`@Filter tenantFilter`), unique `(usuario_id, espaco_evento_id)`.
- `src/main/java/ka/mdo/repository/GestorLocalRepository.java` — `findLocaisDoGestor`, `findGestoresDoLocal`, `findByUsuarioELocal`.
- `src/main/java/ka/mdo/dto/OcupacaoLocalDTO.java` / `EntradaHoraDTO.java` / `PendenciaStatusDTO.java` / `PendenciaLocalDTO.java` — DTOs (records) da resposta agregada.
- `src/main/java/ka/mdo/service/MetricaService.java` — orquestra as 3 métricas com cache em memória TTL 30s (`AtomicReference<ConcurrentHashMap<CacheKey, CacheEntry>>`), valida tenant, valida range (default 24h, max 7d).
- `src/main/java/ka/mdo/service/GestorLocalService.java` — vincular/desvincular idempotente; exige perfil GESTOR_LOCAL no usuário alvo.
- `src/main/java/ka/mdo/resource/MetricaResource.java` — `/metricas/evento/{eventoId}/{ocupacao|entradas|pendencias}` com `@RolesAllowed({"GESTOR_EVENTO","ADMIN_EMPRESA","SUPER_ADMIN","GESTOR_LOCAL"})`.
- `src/main/java/ka/mdo/resource/GestorLocalResource.java` — `POST|DELETE /gestores/{usuarioId}/locais/{localId}` (ADMIN_EMPRESA/SUPER_ADMIN).
- `src/main/resources/db/migration/V15__metricas_vinculo_gestor.sql` — adiciona `LogAcesso.tipoMovimento` (default 'ENTRADA' na migração + drop default) e cria `GestorLocal` com unique composta e 3 índices.

### Arquivos alterados
- `src/main/java/ka/mdo/model/LogAcesso.java` — novo campo `@Enumerated(STRING) TipoMovimento tipoMovimento` default `ENTRADA`.
- `src/main/java/ka/mdo/dto/ValidarAcessoRequestDTO.java` — campo opcional `TipoMovimento tipoMovimento` + construtor compat + helper `tipoMovimentoOuDefault()`.
- `src/main/java/ka/mdo/service/AcessoOcorrido.java` — record ganha `tipoMovimento`.
- `src/main/java/ka/mdo/service/AcessoService.java` — propaga `tipoMovimento` por todos os call-sites de `dispararLog`/`negar`.
- `src/main/java/ka/mdo/service/LogAcessoService.java` — persiste `tipoMovimento`; método `buscar` agora filtra por locais do GESTOR_LOCAL (fecha TODO 030/031).
- `src/main/java/ka/mdo/resource/LogAcessoResource.java` — remove TODO 030/031 e atualiza descrição.
- `src/main/java/ka/mdo/repository/LogAcessoRepository.java` — nova assinatura de `buscar` com `locaisPermitidos`; novos métodos `ocupacaoPorLocal` e `entradasAutorizadasNoPeriodo`.
- `src/main/java/ka/mdo/repository/PendenciaRepository.java` — `buscar` aceita `locaisPermitidos`; novos `totalPorStatus` e `abertasPorLocal`.
- `src/main/java/ka/mdo/pendencia/PendenciaService.java` — `buscar` restringe GESTOR_LOCAL; `notificarGestores` só alcança gestores vinculados ao local da pendência (fecha débito 031).
- `src/main/java/ka/mdo/resource/PendenciaResource.java` — remove TODO 041 da descrição.
- `src/main/java/ka/mdo/resource/AutorizacaoEspacoResource.java` — adiciona `GESTOR_LOCAL` na lista de roles (fecha TODO 041).
- `src/main/java/ka/mdo/service/AutorizacaoEspacoService.java` — em `carregarEspacoDoTenant`, checa que `GESTOR_LOCAL` só opera sobre locais vinculados; 403 caso contrário.
- `PERMISSIONS.md` — 5 rotas novas + atualização do asterisco nas 4 rotas de autorizações + notas 030/031/041 atualizadas.

### Decisão: agregação por hora em Java, não em SQL
`DATE_TRUNC('hour', ts)` existe em Postgres mas não em MariaDB (que usaria `DATE_FORMAT`/`FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(ts)/3600)*3600)`). Para manter portabilidade com o mínimo de SQL dialect-specific, o repositório retorna apenas os `LocalDateTime` crus (filtrados por resultado/tipoMovimento/range) e o `MetricaService` agrupa em memória com `truncatedTo(ChronoUnit.HOURS)`. O range máximo é 7 dias (168h), então o worst-case é dezenas de milhares de linhas — insignificante perto do ganho de simplicidade e portabilidade. Se o volume crescer a ponto de pressionar isso, a otimização natural é criar uma view materializada por DB — decisão que fica para quando houver números reais, não especulação.

### Vínculo gestor↔local: como fechou os TODOs de 030/031
Entidade `GestorLocal` (N:N `Usuario↔EspacoEvento`) com unique composta e filtro `tenantFilter`. Repository expõe `findLocaisDoGestor(usuarioId)` e `findGestoresDoLocal(localId)`. O padrão de detecção "GESTOR_LOCAL puro" (tem o group e NÃO tem SUPER_ADMIN/ADMIN_EMPRESA/GESTOR_EVENTO) foi replicado em 4 lugares — `MetricaService`, `PendenciaService`, `LogAcessoService` e `AutorizacaoEspacoService` — cada um consultando o repository para montar o filtro. A lógica é sempre a mesma: `Collection<Long> locais = null` significa "sem restrição extra" (qualquer perfil acima); lista vazia significa "gestor sem vínculos ⇒ 0 resultados"; lista não vazia vira `local_id IN (...)`. Para pendências/logs sem local (entrada geral), mantemos visibilidade via `(local IS NULL OR local.id IN (...))`. Endpoints `POST|DELETE /gestores/{uid}/locais/{lid}` (ADMIN_EMPRESA/SUPER_ADMIN) criam/removem o vínculo; POST é idempotente. Ao vincular, validamos que o usuário alvo tem `Perfil.GESTOR_LOCAL` (senão 400) — evita vincular silenciosamente perfis sem efeito.

### Endpoint `pendencias` devolve duas visões num só payload
Enunciado pedia contagem por status E por local. Um único `GET /metricas/evento/{id}/pendencias` devolve `{status: [...], porLocal: [...]}` para evitar dois round-trips no dashboard. Cache único (30s) cobre ambas.

### Cache manual vs `quarkus-cache`
Implementação manual (`AtomicReference<ConcurrentHashMap<CacheKey, CacheEntry>>`) — chave inclui eventoId + tipo + bucket(de/ate) + locais do gestor (ordenados para estabilidade). `get` valida TTL lazy; `put` escreve com `now + 30s`. A alternativa seria adicionar `quarkus-cache`, mas a complexidade de config + nova dep não se paga para cache processual volátil.

### Range validation
`de < ate` (400 se violar). `Duration.between(de, ate) > 7 dias` → 400. Default quando não informado: `ate = agora`, `de = agora - 24h`.

### Build status
`./mvnw.cmd compile -q` — **OK** (exit 0; só warnings do runtime Java sobre `sun.misc.Unsafe`).

### Checklist
- ✅ `GET /metricas/evento/{id}/ocupacao` — `OcupacaoLocalDTO`.
- ✅ `GET /metricas/evento/{id}/entradas?granularidade=hora&de=...&ate=...` — `EntradaHoraDTO` (buckets densos, zero-filled).
- ✅ `GET /metricas/evento/{id}/pendencias` — agregado status + local.
- ✅ Cache 30s manual em `MetricaService`.
- ✅ `@RolesAllowed({"GESTOR_EVENTO","ADMIN_EMPRESA","SUPER_ADMIN","GESTOR_LOCAL"})`.
- ✅ `TipoMovimento ENTRADA|SAIDA` propagado por request DTO → record → service → entidade.
- ✅ Migração V15: `ALTER ADD COLUMN tipoMovimento DEFAULT 'ENTRADA'` + `DROP DEFAULT`; tabela `GestorLocal` com unique composta e 3 índices.
- ✅ Vínculo gestor↔local: entidade + repository + service + 2 endpoints.
- ✅ Débitos 030/031 fechados: `GESTOR_LOCAL` em autorizações com validação por vínculo; restrição de visibilidade em logs/pendências/métricas; notificação dirigida.
- ✅ `PERMISSIONS.md` atualizado (5 rotas + `*` nas 4 de autorizações + notas).
- ✅ Validação de datas: `de < ate`, max 7 dias (400).
- ✅ Ocupação protege contra negativos (trata `COUNT(E)-COUNT(S) < 0` como 0).
- ✅ DTOs nunca expõem entidade (records enxutos).
- ❌ Testes de integração — débito consolidado na 051 (roda em paralelo).
- ❌ Granularidade `dia`/`minuto` — fora do escopo; devolve 400 se solicitado.
