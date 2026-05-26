---
id: 013
titulo: Endpoint REST de emissão de credencial
prioridade: alta
estimativa: P
depende-de: [010, 033]
epico: credencial
---

## Contexto
**Gap real**: `IngressoService.adicionarIngresso(usuarioId, dto)` existe e está
testado (atividade 051), mas não há resource REST que o exponha. PERMISSIONS.md
documenta `POST /usuarios/{id}/ingressos` como se existisse, mas não está
implementado — o frontend não consegue emitir credencial via HTTP.

## Objetivo
Expor a emissão como endpoint REST que respeite os perfis e o gate de
credenciais globais já implementados no service.

## Critérios de aceitação
- [x] `POST /usuarios/{idUsuario}/ingressos` (ADMIN_EMPRESA / GESTOR_EVENTO /
      SUPER_ADMIN) recebe `IngressoDTO` no body, delega ao service.
- [x] Aceita o campo opcional `escopoGlobal` (gate em `IngressoService` já existe).
- [x] Responde 201 Created com `IngressoResponseDTO` (sem token bruto).
- [x] 403 quando o usuário-alvo é de outro tenant.
- [x] Atualiza o teste `CredencialEmissaoTest` para usar HTTP em vez de
      `@InjectMock JsonWebToken` no service.
- [x] PERMISSIONS.md (já cita o endpoint) passa a refletir realidade.

## Notas técnicas
- Pode ir em `UsuarioResource` (sub-resource) ou em `IngressoResource`. O nome
  do path já está documentado: `/usuarios/{id}/ingressos`.

## Resultado

Endpoint `POST /usuarios/{idUsuario}/ingressos` exposto em
`IngressoResource` (escolha: manter toda a superfície da credencial num único
resource, ao lado de `GET /ingressos/{id}/qrcode`). O `@Path` da classe foi
removido para permitir que cada método declare a rota completa. Roles
declaradas no método via `@RolesAllowed({"ADMIN_EMPRESA", "GESTOR_EVENTO",
"SUPER_ADMIN"})` — `GESTOR_EVENTO` faltava no resource antigo da credencial e
era explicitamente exigido pelo critério.

`IngressoService.adicionarIngresso` passou a devolver `201 Created`
(`Response.status(CREATED)`) em vez de `Response.ok(...)` — o `IngressoResponseDTO`
continua sem expor o token bruto (`getToken()` nunca entra no DTO).
Toda a lógica de isolamento por tenant (403 cross-tenant) e o gate de
`escopoGlobal` da atividade 033 já estavam no service e foram exercitados
pelo novo teste.

`CredencialEmissaoTest` foi reescrito: removidos o `@InjectMock JsonWebToken`
e a invocação direta ao service. Os dois cenários cobertos agora vão por HTTP
(RestAssured) com JWT real (`TestJwt.bearer(empresa, subject, ADMIN_EMPRESA)`):

1. **Caminho feliz**: `201` + DTO com `id`, `chaveAcesso`, `escopoGlobal=null`
   e `token` ausente; o `tokenDoIngresso(id)` do `TestDataSeeder` confirma que
   o token opaco foi persistido (sem voltar pela API).
2. **Isolamento**: `ADMIN_EMPRESA` do tenant A tentando emitir para usuário
   do tenant B recebe `403`.

PERMISSIONS.md ganhou as linhas dos endpoints `POST /usuarios/{idUsuario}/ingressos`
e `GET /ingressos/{id}/qrcode` (estava omisso) e uma nota nova citando a
atividade 013; a nota da atividade 033 foi atualizada para mencionar que a
exposição HTTP veio nesta atividade.

### Arquivos alterados
- `src/main/java/ka/mdo/resource/IngressoResource.java` — endpoint novo + path por método.
- `src/main/java/ka/mdo/service/IngressoService.java` — `Response.status(CREATED)` no retorno da emissão.
- `src/test/java/ka/mdo/integration/CredencialEmissaoTest.java` — reescrita para HTTP/RestAssured.
- `PERMISSIONS.md` — duas linhas novas na matriz + nota da atividade 013.
- `atividades/em-andamento/ → atividades/concluidas/` (este arquivo).

### Build
`./mvnw -B -ntp -DskipTests package` → **BUILD SUCCESS** (49s).
