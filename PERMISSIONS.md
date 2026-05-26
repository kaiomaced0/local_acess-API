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
| POST | `/auth` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 🔓 |
| POST | `/empresas` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET | `/eventos` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| GET | `/eventos/{id}` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST | `/eventos` | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| POST | `/eventos/espacoevento` | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| PATCH | `/eventos/delete/{id}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET | `/usuarios` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET | `/usuarios/{id}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| POST | `/usuarios` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| PUT | `/usuarios/{id}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| PATCH | `/usuarios/delete/{id}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET | `/usuario-logado` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST | `/acesso/validar` | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| POST | `/acesso/validar-com-foto` | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| GET | `/logs-acesso` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| POST | `/dados-pessoais` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| GET | `/dados-pessoais/meus` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| GET | `/dados-pessoais/{id}` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| POST | `/dados-pessoais/{id}/foto` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST | `/dados-pessoais/{id}/documento-foto` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| GET | `/notificacoes` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST | `/notificacoes/{id}/lida` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| GET | `/notificacoes/nao-lidas/count` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| WS | `/ws/notificacoes` | 🔐 | 🔐 | 🔐 | 🔐 | 🔐 | 🔐 | ❌ |
| GET | `/espacos-evento/{espacoId}/autorizacoes` | ✅ | ✅ | ✅ | ✅* | ❌ | ❌ | ❌ |
| PUT | `/espacos-evento/{espacoId}/autorizacoes` | ✅ | ✅ | ✅ | ✅* | ❌ | ❌ | ❌ |
| POST | `/espacos-evento/{espacoId}/autorizacoes/{tipoIngressoId}` | ✅ | ✅ | ✅ | ✅* | ❌ | ❌ | ❌ |
| DELETE | `/espacos-evento/{espacoId}/autorizacoes/{tipoIngressoId}` | ✅ | ✅ | ✅ | ✅* | ❌ | ❌ | ❌ |
| GET | `/pendencias` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| POST | `/pendencias/{id}/aprovar` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| POST | `/pendencias/{id}/recusar` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| GET | `/eventos/{eventoId}/mapa` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| PUT | `/eventos/{eventoId}/mapa` | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| POST | `/eventos/{eventoId}/mapa/imagem-fundo` | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| GET | `/metricas/evento/{eventoId}/ocupacao` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| GET | `/metricas/evento/{eventoId}/entradas` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| GET | `/metricas/evento/{eventoId}/pendencias` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| POST | `/gestores/{usuarioId}/locais/{localId}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| DELETE | `/gestores/{usuarioId}/locais/{localId}` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |

## Notas

- `SUPER_ADMIN` nunca tem o filtro Hibernate por tenant ativado — ver
  `TenantRequestFilter`. Isso permite operações cross-tenant (criar empresas,
  auditar dados). Demais perfis SEMPRE dependem do claim `empresaId` no JWT
  (401 "Token sem empresaId" se ausente).
- `@Authenticated` é usado em `/eventos` e `/usuario-logado`
  quando qualquer perfil autenticado pode acessar — a restrição por tenant é
  delegada ao filtro Hibernate + validações no service.
- Rotas versionadas sob `/*` a partir da atividade 052. Breaking
  changes futuros entram em `/api/v2` sem quebrar clientes atuais.
- Endpoints de OpenAPI (`/q/openapi`) e health (`/q/health`) do Quarkus são
  públicos por default (extensão não restringe).
- Atualize esta tabela sempre que um endpoint novo for adicionado ou um
  `@RolesAllowed` for alterado.
- Atividade 030 (autorizações por local): `GESTOR_LOCAL` tem acesso aos
  4 endpoints `/espacos-evento/{id}/autorizacoes`, porém APENAS
  sobre locais vinculados a ele via `GestorLocal` (atividade 041). Fora
  dos locais vinculados o service responde 403. O `*` na tabela acima
  sinaliza essa restrição condicional.
- Atividade 032 (notificações): o websocket `/ws/notificacoes` não
  usa `@RolesAllowed` — o handshake é público, mas a sessão fica em estado
  não autenticado até receber `AUTH <jwt>` como primeira mensagem
  (ver `WebsocketChannel`). Qualquer outra mensagem inicial fecha a
  conexão. Após autenticado, só recebe notificações destinadas ao próprio
  usuário (subject do JWT).
- Atividade 033 (credenciais globais): o endpoint de emissão
  (`POST /usuarios/{id}/ingressos`) aceita o campo opcional
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
  `/eventos/{eventoId}/mapa`. Leitura aberta a todos os perfis
  autenticados do tenant (aparelhos e clientes precisam do mapa para
  visualização). Escrita (PUT do mapa e upload da imagem de fundo)
  restrita a `GESTOR_EVENTO`, `ADMIN_EMPRESA` e `SUPER_ADMIN`. Cada
  `FormaMapa` referencia um `EspacoEvento` que DEVE pertencer ao mesmo
  evento e ao mesmo tenant — cross-evento é 400, cross-tenant é 403.
  Imagens de fundo vão para um bucket novo `mapas` (separado de
  `credenciais-foto`/`documentos`), criado automaticamente pelo
  `StorageBucketBootstrap`.
- Atividade 031 (workflow de pendências): as rotas `/pendencias` são
  restritas a `GESTOR_EVENTO`, `GESTOR_LOCAL`, `ADMIN_EMPRESA` e
  `SUPER_ADMIN`. Desde a atividade 041, `GESTOR_LOCAL` enxerga apenas
  pendências dos locais vinculados a ele via `GestorLocal` (e pendências
  sem local — aparelho de entrada geral). Notificações a gestores de
  local também respeitam o vínculo. Criação de pendência é automática
  (observer do evento CDI `PendenciaRequerida`, disparado pelo
  `AcessoService` / `FacialValidationService`) — não há endpoint público
  para abrir pendência manualmente.
- Atividade 041 (dashboards do gestor): três endpoints
  `GET /metricas/evento/{id}/{ocupacao|entradas|pendencias}` com
  cache em memória de 30s. `GESTOR_LOCAL` tem a visão restringida aos
  locais vinculados. Entradas aceita `de` e `ate` (default 24h, max 7
  dias — 400 se extrapolar) e `granularidade=hora`. Também introduz a
  entidade `GestorLocal` e os endpoints
  `POST|DELETE /gestores/{usuarioId}/locais/{localId}` (restritos
  a `ADMIN_EMPRESA` / `SUPER_ADMIN`), que fecham os débitos 030 e 031 de
  visibilidade do `GESTOR_LOCAL`. Adicionada a coluna
  `LogAcesso.tipoMovimento` (`ENTRADA`/`SAIDA`, default `ENTRADA` em
  migração + drop default) usada no cálculo de ocupação.
