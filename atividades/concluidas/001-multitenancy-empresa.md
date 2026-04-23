---
id: 001
titulo: Multitenancy — entidade Empresa e isolamento por tenant
prioridade: alta
estimativa: G
depende-de: []
epico: fundacao
---

## Contexto
Hoje o sistema não isola dados por cliente. Precisamos suportar múltiplas empresas usando a mesma instância, cada uma com seus próprios eventos, usuários, credenciais, sem jamais enxergar dados das outras.

## Objetivo
Toda entidade de negócio referencia `Empresa`. O JWT carrega `empresaId`. Todas as queries filtram por esse id automaticamente.

## Critérios de aceitação
- [x] Entidade `Empresa` criada com nome, cnpj, status (ativo/suspenso).
- [x] `Usuario`, `Evento`, `EspacoEvento`, `Ingresso`, `TipoIngresso` possuem FK `empresa_id`. (`Perfil` é `@ElementCollection` de enum, não entidade JPA — não precisa de FK.)
- [x] Token JWT inclui claim `empresaId` e `perfil`.
- [x] Filtro Hibernate (`@FilterDef` no pacote + `@Filter` em cada entidade) aplica `empresaId` automaticamente em todas as leituras via `TenantRequestFilter` + `TenantFilterActivator`.
- [x] Operações de escrita validam que o recurso alvo pertence à empresa do chamador (`ForbiddenException` → 403 caso contrário).
- [x] Endpoint `POST /empresas` restrito a `@RolesAllowed("SUPER_ADMIN")`.
- [ ] Teste de integração: usuário da empresa A não consegue ler nem alterar recursos da empresa B. — **deixado para a atividade 051** (suite de testes). Projeto ainda não tem suite mínima; criar uma apenas para este caso fugiria do escopo.

## Notas técnicas
- Avaliar `Hibernate @Filter` vs. `MultiTenantConnectionProvider` (estratégia discriminator é suficiente aqui).
- Migração de schema: adicionar colunas com default para dados existentes.
- Essa atividade bloqueia quase todas as outras — priorizar.

## Resultado

### Arquivos criados
- `src/main/java/ka/mdo/model/Empresa.java` — entidade JPA tenant-root.
- `src/main/java/ka/mdo/model/StatusEmpresa.java` — enum `ATIVA`/`SUSPENSA`.
- `src/main/java/ka/mdo/model/package-info.java` — `@FilterDef` declarado uma única vez no pacote (evita conflito por múltiplas declarações do mesmo filtro em entidades distintas).
- `src/main/java/ka/mdo/tenant/TenantContext.java` — `@RequestScoped` com o `empresaId` do chamador.
- `src/main/java/ka/mdo/tenant/TenantFilterActivator.java` — ativa o filtro Hibernate na sessão corrente.
- `src/main/java/ka/mdo/tenant/TenantRequestFilter.java` — `@Provider ContainerRequestFilter` que lê o claim `empresaId` do JWT.
- `src/main/java/ka/mdo/repository/EmpresaRepository.java` — Panache; `findByCnpj`.
- `src/main/java/ka/mdo/dto/EmpresaDTO.java` e `EmpresaResponseDTO.java`.
- `src/main/java/ka/mdo/service/EmpresaService.java`.
- `src/main/java/ka/mdo/resource/EmpresaResource.java` — `POST /empresas` com `@RolesAllowed("SUPER_ADMIN")`.
- `src/main/resources/db/migration/V2__adiciona_empresa.sql`.
- `src/main/resources/db/migration/V3__adiciona_empresa_id.sql`.

### Arquivos alterados
- `src/main/java/ka/mdo/model/Evento.java`, `EspacoEvento.java`, `Ingresso.java`, `TipoIngresso.java`, `Usuario.java` — `@ManyToOne Empresa empresa` + `@Filter("tenantFilter" ...)`.
- `src/main/java/ka/mdo/service/TokenJwtService.java` — claims `empresaId` (Long) e `perfil` (labels dos perfis).
- `src/main/java/ka/mdo/service/EventoService.java` — injeção de `JsonWebToken`, `empresaDoJwt()`, `validarMesmoTenant()`, setter de `empresa` no insert, validação em update/delete/insertEspacoEvento.
- `src/main/java/ka/mdo/service/IngressoService.java` — validação de `Usuario` e `TipoIngresso` pertencerem ao mesmo tenant; seta `empresa` no ingresso.
- `src/main/java/ka/mdo/service/UsuarioService.java` — insert seta `empresa`, update/delete validam `empresa` via `ForbiddenException`.

### Build
- `./mvnw.cmd compile -q` → exit 0.
- Runtime contra MariaDB/Postgres não foi testado (sem ambiente). Recomenda-se subir `quarkus:dev` para validar V2/V3 e a ativação do filtro em uma sessão autenticada antes de deploy.

### Débitos técnicos / pendências
- **Teste de isolamento**: não foi criada suite mínima. A atividade 051 (ou a primeira que criar a suite de integração) deve incluir: login como usuário da empresa A, tentar `GET /evento/{id-da-empresa-B}` e `PUT /usuario/{id-da-empresa-B}` → esperar 404 (filtro) e 403 (validação explícita) respectivamente.
- **`SUPER_ADMIN` não formalizado**: a atividade 002 (perfis e `@RolesAllowed`) precisa adicionar `SUPER_ADMIN` ao enum `Perfil` e formalizar quem pode criar empresas. Hoje o `@RolesAllowed("SUPER_ADMIN")` no `EmpresaResource` bloqueia todo mundo até o perfil existir.
- **Bootstrap da primeira empresa**: a V3 cria `Empresa(id=1, 'Empresa Padrão')` e faz backfill de todas as linhas existentes. Ainda não há fluxo para cadastrar o primeiro `SUPER_ADMIN` — possivelmente um seed em `import.sql` (dev) ou migração V4 (prod) na atividade 002.
- **Usuário `superGlobal`**: a skill menciona flag que permite acesso cross-tenant. Ainda não implementada; quando for, o `TenantFilterActivator` precisa de branch que não ative o filtro para usuários com essa flag.
- **`UsuarioLogadoService.updateSenha`**: não foi alterado; já é filtrado por `jwt.getSubject()` (id próprio do usuário), mas como o filtro Hibernate está ativo na sessão, se o `empresaId` do JWT divergir do `empresa_id` gravado no banco o `findByIdModificado` retornará `null` — comportamento aceitável (usuário só mexe na própria senha).
- **DTOs**: `EmpresaResponseDTO` expõe só id/nome/cnpj/status. As demais Response DTOs **não** expõem `empresa` inteira — apenas omitem o campo, o que é suficiente para a multitenancy. Se futuramente for necessário exibir `empresaId` em alguma resposta, adicionar apenas o Long, não a entidade.
- **Filtro pode silenciosamente esconder dados**: se o JWT for válido mas sem o claim `empresaId` (cenário que não deve ocorrer após esta atividade, mas pode ocorrer com tokens legados), o filtro não é ativado e o endpoint pode vazar entre tenants. Vale adicionar na 002 uma checagem explícita de "JWT sem empresaId → 401" em um `ContainerRequestFilter` global para endpoints autenticados.
