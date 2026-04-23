# Matriz de permissões — local_acess-API

Gerada pela atividade 002. Reflete os `@RolesAllowed`/`@PermitAll`/`@Authenticated`
aplicados em `src/main/java/ka/mdo/resource/*.java`.

Legenda:
- ✅ Acesso permitido.
- ❌ Acesso negado (401/403).
- 🔓 Público (não exige autenticação).
- 🔐 Exige autenticação, sem restrição de perfil.

## Perfis

| Sigla | Enum | Descrição |
|-------|------|-----------|
| SA | `SUPER_ADMIN` | Administrador da instância; cross-tenant (sem filtro Hibernate). |
| AE | `ADMIN_EMPRESA` | Administrador da empresa (tenant). |
| GE | `GESTOR_EVENTO` | Gerencia eventos e espaços da empresa. |
| GL | `GESTOR_LOCAL` | Gerencia locais/espaços da empresa. |
| OA | `OPERADOR_APARELHO` | Opera dispositivos de validação. |
| CL | `CLIENTE` | Usuário final (comprador de ingresso). |
| — | (anônimo) | Sem token. |

## Endpoints

| Método | Path | SA | AE | GE | GL | OA | CL | Anônimo |
|--------|------|----|----|----|----|----|----|---------|
| POST | `/api/v1/auth` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 🔓 |
| POST | `/api/v1/empresas` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET | `/api/v1/eventos` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| GET | `/api/v1/eventos/{id}` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST | `/api/v1/eventos` | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| POST | `/api/v1/eventos/espacoevento` | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| PATCH | `/api/v1/eventos/delete/{id}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET | `/api/v1/usuarios` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET | `/api/v1/usuarios/{id}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| POST | `/api/v1/usuarios` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| PUT | `/api/v1/usuarios/{id}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| PATCH | `/api/v1/usuarios/delete/{id}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET | `/api/v1/usuario-logado` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST | `/api/v1/acesso/validar` | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| POST | `/api/v1/acesso/validar-com-foto` | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| GET | `/api/v1/logs-acesso` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| POST | `/api/v1/dados-pessoais` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| GET | `/api/v1/dados-pessoais/meus` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| GET | `/api/v1/dados-pessoais/{id}` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| POST | `/api/v1/dados-pessoais/{id}/foto` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST | `/api/v1/dados-pessoais/{id}/documento-foto` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| GET | `/api/v1/notificacoes` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST | `/api/v1/notificacoes/{id}/lida` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| GET | `/api/v1/notificacoes/nao-lidas/count` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| WS | `/api/v1/ws/notificacoes` | 🔐 | 🔐 | 🔐 | 🔐 | 🔐 | 🔐 | ❌ |
| GET | `/api/v1/espacos-evento/{espacoId}/autorizacoes` | ✅ | ✅ | ✅ | ✅* | ❌ | ❌ | ❌ |
| PUT | `/api/v1/espacos-evento/{espacoId}/autorizacoes` | ✅ | ✅ | ✅ | ✅* | ❌ | ❌ | ❌ |
| POST | `/api/v1/espacos-evento/{espacoId}/autorizacoes/{tipoIngressoId}` | ✅ | ✅ | ✅ | ✅* | ❌ | ❌ | ❌ |
| DELETE | `/api/v1/espacos-evento/{espacoId}/autorizacoes/{tipoIngressoId}` | ✅ | ✅ | ✅ | ✅* | ❌ | ❌ | ❌ |
| GET | `/api/v1/pendencias` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| POST | `/api/v1/pendencias/{id}/aprovar` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| POST | `/api/v1/pendencias/{id}/recusar` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| GET | `/api/v1/eventos/{eventoId}/mapa` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| PUT | `/api/v1/eventos/{eventoId}/mapa` | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| POST | `/api/v1/eventos/{eventoId}/mapa/imagem-fundo` | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| GET | `/api/v1/metricas/evento/{eventoId}/ocupacao` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| GET | `/api/v1/metricas/evento/{eventoId}/entradas` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| GET | `/api/v1/metricas/evento/{eventoId}/pendencias` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| POST | `/api/v1/gestores/{usuarioId}/locais/{localId}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| DELETE | `/api/v1/gestores/{usuarioId}/locais/{localId}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |

## Notas

- `SUPER_ADMIN` nunca tem o filtro Hibernate por tenant ativado — ver
  `TenantRequestFilter`. Isso permite operações cross-tenant (criar empresas,
  auditar dados). Demais perfis SEMPRE dependem do claim `empresaId` no JWT
  (401 "Token sem empresaId" se ausente).
- `@Authenticated` é usado em `/api/v1/eventos` e `/api/v1/usuario-logado`
  quando qualquer perfil autenticado pode acessar — a restrição por tenant é
  delegada ao filtro Hibernate + validações no service.
- Rotas versionadas sob `/api/v1/*` a partir da atividade 052. Breaking
  changes futuros entram em `/api/v2` sem quebrar clientes atuais.
- Endpoints de OpenAPI (`/q/openapi`) e health (`/q/health`) do Quarkus são
  públicos por default (extensão não restringe).
- Atualize esta tabela sempre que um endpoint novo for adicionado ou um
  `@RolesAllowed` for alterado.
- Atividade 030 (autorizações por local): `GESTOR_LOCAL` tem acesso aos
  4 endpoints `/api/v1/espacos-evento/{id}/autorizacoes`, porém APENAS
  sobre locais vinculados a ele via `GestorLocal` (atividade 041). Fora
  dos locais vinculados o service responde 403. O `*` na tabela acima
  sinaliza essa restrição condicional.
- Atividade 032 (notificações): o websocket `/api/v1/ws/notificacoes` não
  usa `@RolesAllowed` — o handshake é público, mas a sessão fica em estado
  não autenticado até receber `AUTH <jwt>` como primeira mensagem
  (ver `WebsocketChannel`). Qualquer outra mensagem inicial fecha a
  conexão. Após autenticado, só recebe notificações destinadas ao próprio
  usuário (subject do JWT).
- Atividade 033 (credenciais globais): o endpoint de emissão
  (`POST /api/v1/usuarios/{id}/ingressos`) aceita o campo opcional
  `escopoGlobal` no request. Gate de emissão aplicado no `IngressoService`:
  - `escopoGlobal=SUPER` (cross-tenant): somente `SUPER_ADMIN` pode emitir.
  - `escopoGlobal=EMPRESA`: somente `ADMIN_EMPRESA` ou `SUPER_ADMIN`.
  - `escopoGlobal=null` (default): regras atuais de emissão.
  Na validação de acesso, credenciais globais curto-circuitam checagens de
  perfil/local:
  - `SUPER`: pula tenant, período, local, perfil, dados pessoais e facial
    — autoriza direto. Busca cross-tenant via
    `IngressoRepository.findByTokenCrossTenant` (desativa o `tenantFilter`
    somente para esta query).
  - `EMPRESA`: mantém tenant (aparelho x credencial), período, dados
    pessoais e facial; pula apenas a whitelist de `TipoIngresso` por
    local (atividade 030).
  Cada decisão originada por credencial global é marcada no `LogAcesso`
  com `acessoGlobal=true` para auditoria destacada. **Débito técnico:**
  exigir 2FA antes de emitir credencial `SUPER` (não implementado nesta
  atividade).
- Atividade 040 (mapa 2D do evento): rotas sob
  `/api/v1/eventos/{eventoId}/mapa`. Leitura aberta a todos os perfis
  autenticados do tenant (aparelhos e clientes precisam do mapa para
  visualização). Escrita (PUT do mapa e upload da imagem de fundo)
  restrita a `GESTOR_EVENTO`, `ADMIN_EMPRESA` e `SUPER_ADMIN`. Cada
  `FormaMapa` referencia um `EspacoEvento` que DEVE pertencer ao mesmo
  evento e ao mesmo tenant — cross-evento é 400, cross-tenant é 403.
  Imagens de fundo vão para um bucket novo `mapas` (separado de
  `credenciais-foto`/`documentos`), criado automaticamente pelo
  `StorageBucketBootstrap`.
- Atividade 031 (workflow de pendências): as rotas `/api/v1/pendencias` são
  restritas a `GESTOR_EVENTO`, `GESTOR_LOCAL`, `ADMIN_EMPRESA` e
  `SUPER_ADMIN`. Desde a atividade 041, `GESTOR_LOCAL` enxerga apenas
  pendências dos locais vinculados a ele via `GestorLocal` (e pendências
  sem local — aparelho de entrada geral). Notificações a gestores de
  local também respeitam o vínculo. Criação de pendência é automática
  (observer do evento CDI `PendenciaRequerida`, disparado pelo
  `AcessoService` / `FacialValidationService`) — não há endpoint público
  para abrir pendência manualmente.
- Atividade 041 (dashboards do gestor): três endpoints
  `GET /api/v1/metricas/evento/{id}/{ocupacao|entradas|pendencias}` com
  cache em memória de 30s. `GESTOR_LOCAL` tem a visão restringida aos
  locais vinculados. Entradas aceita `de` e `ate` (default 24h, max 7
  dias — 400 se extrapolar) e `granularidade=hora`. Também introduz a
  entidade `GestorLocal` e os endpoints
  `POST|DELETE /api/v1/gestores/{usuarioId}/locais/{localId}` (restritos
  a `ADMIN_EMPRESA` / `SUPER_ADMIN`), que fecham os débitos 030 e 031 de
  visibilidade do `GESTOR_LOCAL`. Adicionada a coluna
  `LogAcesso.tipoMovimento` (`ENTRADA`/`SAIDA`, default `ENTRADA` em
  migração + drop default) usada no cálculo de ocupação.
