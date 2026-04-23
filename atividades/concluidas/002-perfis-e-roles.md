---
id: 002
titulo: Perfis de usuário e @RolesAllowed nos resources
prioridade: alta
estimativa: M
depende-de: [001]
epico: fundacao
---

## Contexto
`Perfil` existe como entidade mas não há um conjunto claro de roles, nem `@RolesAllowed` aplicado nos endpoints.

## Objetivo
Formalizar perfis do sistema e proteger endpoints de acordo.

## Critérios de aceitação
- [ ] Enum ou constantes com: `SUPER_ADMIN`, `ADMIN_EMPRESA`, `GESTOR_EVENTO`, `GESTOR_LOCAL`, `OPERADOR_APARELHO`, `CLIENTE`.
- [ ] Claim `groups` do JWT carrega os perfis do usuário.
- [ ] Todos os resources aplicam `@RolesAllowed`; público só o necessário (login, health).
- [ ] Matriz de permissão documentada em comentário no `AuthResource` ou arquivo `PERMISSIONS.md`.
- [ ] Teste: cliente não consegue acessar endpoints de gestor; gestor de local não altera evento.

## Notas técnicas
- `TokenJwtService` precisa emitir `groups`.
- Revisar `Perfil` (entidade) — pode virar tabela de associação usuario↔role.

## Resultado

### Arquivos criados
- `src/main/java/ka/mdo/service/BootstrapService.java` — `@Observes StartupEvent` cria o primeiro `SUPER_ADMIN` se não existir.
- `src/main/resources/db/migration/V4__renomeia_perfis.sql` — renomeia valores legados do enum.
- `PERMISSIONS.md` (raiz) — matriz de permissões por perfil × endpoint.

### Arquivos alterados
- `src/main/java/ka/mdo/model/Perfil.java` — substituído por enum formal `{SUPER_ADMIN, ADMIN_EMPRESA, GESTOR_EVENTO, GESTOR_LOCAL, OPERADOR_APARELHO, CLIENTE}`. Removido campo `id` (não havia uso consistente com o schema VARCHAR).
- `src/main/java/ka/mdo/model/Usuario.java` — `@Enumerated(EnumType.STRING)` adicionado à `@ElementCollection perfis` (consistente com a coluna VARCHAR(30) de `usuario_perfil`).
- `src/main/java/ka/mdo/service/TokenJwtService.java` — emite `groups` (`Set<String>`) usando `Perfil.name()` para casar com `@RolesAllowed("SUPER_ADMIN")`; mantém `perfil` com labels.
- `src/main/java/ka/mdo/service/UsuarioService.java` — default `Perfil.USER` trocado para `Perfil.CLIENTE`.
- `src/main/java/ka/mdo/tenant/TenantRequestFilter.java` — reforçado: `SUPER_ADMIN` pula filtro Hibernate (cross-tenant); JWT autenticado sem `empresaId` e sem `SUPER_ADMIN` aborta com 401 "Token sem empresaId".
- `src/main/java/ka/mdo/resource/EmpresaResource.java` — `@RolesAllowed("SUPER_ADMIN")` no tipo.
- `src/main/java/ka/mdo/resource/UsuarioResource.java` — `@RolesAllowed({"SUPER_ADMIN", "ADMIN_EMPRESA"})`.
- `src/main/java/ka/mdo/resource/UsuarioLogadoResource.java` — `@Authenticated`.
- `src/main/java/ka/mdo/resource/EventoResource.java` — `@Authenticated` no tipo; criação/edição exige `ADMIN_EMPRESA`/`GESTOR_EVENTO` (+SA); exclusão exige `ADMIN_EMPRESA` (+SA).
- `src/main/resources/application.properties` — adicionadas propriedades `bootstrap.super-admin.*` (email, senha, cpf, nome).

### Arquivos removidos
- `src/main/java/ka/mdo/converter/PerfilConverter.java` — convertia `Perfil <-> Integer`, mas o schema era `VARCHAR(30)`. Incompatível e obsoleto após a mudança do enum.

### Mapeamentos de perfis legados (migração V4)
| Legado | Novo |
|--------|------|
| `ADMIN` / `Admin` / `1` | `ADMIN_EMPRESA` |
| `USER` / `User` / `2` | `CLIENTE` |
| `MEDICO` / `Medico` / `3` | `CLIENTE` (não havia equivalente direto) |

### Decisão: bootstrap via StartupEvent vs migração SQL
Escolhido **StartupEvent** porque o hash de senha depende do `HashService` (PBKDF2 com sal `#blahhxyz9232` e 223 iterações). Hardcodar hash em migração SQL quebraria ao qualquer mudança desses parâmetros e duplicaria lógica fora da JVM. O bean roda idempotente: só insere se `COUNT(Usuario.perfis = SUPER_ADMIN) == 0`.

### Decisão: empresa do SUPER_ADMIN
A coluna `Usuario.empresa_id` é `NOT NULL` (V3 — atividade 001). Para não quebrar a integridade referencial, o SUPER_ADMIN bootstrap é atrelado à Empresa Padrão (id=1) criada pela V3. A semântica cross-tenant é garantida pelo `TenantRequestFilter` que **não ativa** o filtro Hibernate quando o JWT contém o grupo `SUPER_ADMIN`, mesmo havendo `empresaId` no token.

### Build
- `./mvnw.cmd compile -q` → exit 0.
- Runtime contra MariaDB/PostgreSQL não foi exercitado. Recomenda-se `quarkus:dev` para validar V4 e o StartupEvent.

### Critérios de aceitação
- [x] Enum com os 6 perfis formais.
- [x] Claim `groups` no JWT com os perfis do usuário.
- [x] Todos os resources aplicam `@RolesAllowed`/`@Authenticated`; público só o login.
- [x] Matriz de permissão documentada (`PERMISSIONS.md`).
- [ ] Teste de integração (cliente não acessa endpoints de gestor etc.) — **delegado à atividade 051** (criação da suite).

### Débitos da 001 fechados
- [x] `SUPER_ADMIN` formalizado no enum `Perfil`.
- [x] Filtro global rejeita JWT autenticado sem `empresaId` com 401 (`TenantRequestFilter`).
- [x] Bootstrap do primeiro `SUPER_ADMIN` via `StartupEvent` (`BootstrapService`).

### Débitos remanescentes
- **Suite de testes** (atividade 051): validar matriz de permissões por perfil e o isolamento multitenant.
- **Flag `superGlobal` no `Usuario`**: ainda não existe; quando existir, o `TenantFilterActivator` precisa tratar também esse caso.
- **Troca de senha exposta**: `UsuarioLogadoService.updateSenha` existe mas não tem endpoint. Fora de escopo.
- **Validação de `StatusEmpresa` em runtime**: filtro não bloqueia empresa `SUSPENSA`. Avaliar em atividade futura.
- **`q/health` / `q/openapi`**: públicos por default do Quarkus. Se a política de segurança exigir, restringir via `quarkus.http.auth.permission.*`.
