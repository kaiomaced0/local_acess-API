---
id: 030
titulo: Lista de perfis autorizados por local (alterável em tempo real)
prioridade: alta
estimativa: M
depende-de: [002]
epico: locais
---

## Contexto
Cada `EspacoEvento` define quais perfis de credencial podem entrar. Durante o evento, o gestor pode mudar essa lista (ex: liberar VIP para todos após 22h).

## Objetivo
Relacionamento N:N entre `EspacoEvento` e perfis/tipos de ingresso autorizados, editável em runtime.

## Critérios de aceitação
- [x] Tabela de associação `espacoevento_tipo_ingresso_autorizado`.
- [x] Endpoints: `PUT /espacos-evento/{id}/autorizacoes` (substitui), `POST` (adiciona), `DELETE` (remove) + `GET` de listagem.
- [x] Restrito a `GESTOR_EVENTO`, `ADMIN_EMPRESA`, `SUPER_ADMIN`. `GESTOR_LOCAL` delegado à 041 (vínculo gestor↔local ainda não modelado).
- [x] Mudanças refletem imediatamente na próxima validação — `AcessoService` consulta o banco sem cache.
- [x] Log de auditoria (`AutorizacaoAuditoria`) persistido a cada mudança.

## Notas técnicas
- Decidir se autorização é por `TipoIngresso` ou por um novo conceito `PerfilCredencial`. Documentar no PR.

## Resultado

### Decisão: TipoIngresso vs PerfilCredencial
Optamos por **`TipoIngresso`** (reutilização). `TipoIngresso` já é multitenant, já é referenciado por toda `Credencial`/`Ingresso`, e cobre os casos comuns do domínio (VIP, Staff, Inteira). Criar um `PerfilCredencial` agora seria complexidade antecipada: precisaríamos de um segundo cadastro + mapeamento `TipoIngresso → PerfilCredencial`, sem ganho imediato. Quando surgir necessidade real (ex.: mesmo tipo de ingresso com perfis distintos por evento, staff de terceirizados), a migração é aditiva — basta introduzir uma segunda tabela de associação sem quebrar a atual.

### Arquivos criados
- `src/main/java/ka/mdo/model/AcaoAutorizacao.java` — enum `ADICIONADO | REMOVIDO | SUBSTITUIDO`.
- `src/main/java/ka/mdo/model/AutorizacaoAuditoria.java` — entidade multitenant (`@Filter tenantFilter`), FKs para `Empresa`/`EspacoEvento`, `tipoIngressoId` (nullable para SUBSTITUIDO), `usuarioId` soft (sem FK) e `dataHora`.
- `src/main/java/ka/mdo/dto/AutorizacaoDTO.java` — request do PUT.
- `src/main/java/ka/mdo/dto/AutorizacaoResponseDTO.java` — response com `TipoIngressoResumo(id, nome)` (nunca expõe a entidade).
- `src/main/java/ka/mdo/repository/AutorizacaoAuditoriaRepository.java`.
- `src/main/java/ka/mdo/service/AutorizacaoEspacoService.java` — `listar` / `adicionar` / `remover` / `substituir`, multitenant + auditoria na mesma transação.
- `src/main/java/ka/mdo/resource/AutorizacaoEspacoResource.java` — 4 endpoints em `/espacos-evento/{espacoId}/autorizacoes`.
- `src/main/resources/db/migration/V11__autorizacoes_locais.sql` — tabela de associação (PK composta + índice inverso) e `AutorizacaoAuditoria` (índices por empresa/data e por espaço/data).

### Arquivos alterados
- `src/main/java/ka/mdo/model/EspacoEvento.java` — `@ManyToMany(FetchType.LAZY) Set<TipoIngresso> tiposIngressoAutorizados` com `@JoinTable`.
- `src/main/java/ka/mdo/repository/EspacoEventoRepository.java` — `contarTiposAutorizados(espacoId)` e `contemTipoAutorizado(espacoId, tipoId)` via JPQL (evita carregar a coleção LAZY inteira).
- `src/main/java/ka/mdo/service/AcessoService.java` — TODO 030 fechado; novo motivo `PERFIL_NAO_AUTORIZADO_NO_LOCAL`; javadoc atualizado.
- `PERMISSIONS.md` — 4 linhas novas + nota sobre `GESTOR_LOCAL` pendente para 041.

### Como fechou o TODO 030 em AcessoService
No passo de validação de local (`if (req.localId() != null)`):
1. Consulta `espacoEventoRepository.contarTiposAutorizados(local.getId())`.
2. Se `0`, **autoriza** (lista vazia = sem restrição).
3. Se `> 0`, consulta `contemTipoAutorizado(local.getId(), ingresso.getTipoIngresso().getId())`. Se falso (ou `tipoIngresso == null`), devolve `negar(...)` com motivo `PERFIL_NAO_AUTORIZADO_NO_LOCAL` — que já dispara `dispararLog(...)` para auditoria assíncrona.
4. As duas consultas são SQL puro (não inicializam a coleção LAZY), evitando N+1 e não dependendo do escopo de sessão Hibernate.

### Política para lista vazia: autoriza
Escolhemos **autorizar** quando `tiposIngressoAutorizados` é vazia. Justificativa: a configuração é opt-in; obrigar o gestor a cadastrar a whitelist em cada local para liberar acesso quebraria eventos já em operação que ainda não conhecem o recurso. "Lista vazia = todos os tipos" é o default menos surpreendente e reversível — basta adicionar um tipo para ativar a restrição. Se algum cliente preferir o contrário (fail-closed), evoluímos via flag por tenant na atividade 041 sem mudar o schema atual.

### Build status
`./mvnw.cmd compile -q` — **OK** (exit 0; apenas warnings do runtime Java sobre `sun.misc.Unsafe` em deps transitivas do Maven).

### Checklist
- ✅ Entidade `EspacoEvento` com `@ManyToMany(FetchType.LAZY) Set<TipoIngresso>` + `@JoinTable` conforme especificado.
- ✅ Entidade `AutorizacaoAuditoria` (multitenant + `AcaoAutorizacao` enum STRING).
- ✅ DTOs request/response com validação Jakarta.
- ✅ Service `AutorizacaoEspacoService` com `listar`/`adicionar`/`remover`/`substituir` — tenant-check em cada operação e em cada `TipoIngresso` referenciado.
- ✅ Resource `AutorizacaoEspacoResource` com 4 endpoints, `@RolesAllowed`, `@Tag`/`@Operation`/`@APIResponse`.
- ✅ TODO 030 fechado em `AcessoService` (+ motivo `PERFIL_NAO_AUTORIZADO_NO_LOCAL`, log assíncrono via `negar(...)`).
- ✅ Política lista vazia = autoriza (documentada).
- ✅ Migração `V11__autorizacoes_locais.sql` portável (MariaDB/Postgres).
- ✅ `PERMISSIONS.md` atualizado.
- ✅ Acesso LAZY evita N+1 (consultas dedicadas na validação, coleção só materializada nas operações de escrita dentro de `@Transactional`).
- ✅ Build passa.
- ❌ Testes de integração (débito consolidado na 051, coerente com 011/020/021).
- ❌ `GESTOR_LOCAL` no resource (delegado à 041, junto com vínculo gestor↔local).
- ❌ Endpoints de consulta da auditoria (delegado à 041 — entidade persiste desde já para popular dashboards futuros).
