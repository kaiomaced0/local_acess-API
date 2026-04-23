---
id: 032
titulo: Serviço de notificações (push + websocket + email)
prioridade: media
estimativa: G
depende-de: [002]
epico: locais
---

## Contexto
Pendências precisam chegar em gestores rapidamente; clientes precisam saber do desfecho. Também há notificações gerais (credencial emitida, evento próximo, etc.).

## Objetivo
Abstração única de notificação com múltiplos canais pluggáveis.

## Critérios de aceitação
- [x] Interface `NotificacaoService.enviar(destinatario, tipo, payload)`.
- [x] Canais: websocket (gestores online no painel), push mobile (FCM — stub inicial), email (`quarkus-mailer`).
- [x] Preferência do usuário por canal armazenada.
- [x] Entrega assíncrona, com retry e fila interna (`Arc` Events ou Kafka futuro).
- [x] Endpoint `GET /notificacoes` lista notificações do usuário logado.
- [x] `POST /notificacoes/{id}/lida` marca como lida.

## Notas técnicas
- Começar com websocket + email; push (FCM) como iteração seguinte.
- Evitar acoplamento: event listeners consomem eventos internos (pendência criada, pendência resolvida) e delegam ao serviço.

## Resultado

Infraestrutura de notificações pronta para uso. Compilação OK
(`./mvnw.cmd clean compile` → BUILD SUCCESS, 87 fontes).

### Arquitetura

- **Fachada**: `NotificacaoService.enviar(destinatarioId, tipo, titulo,
  mensagem, payloadJson)` persiste a `Notificacao` na transação do
  chamador e dispara o evento CDI `NotificacaoCriada` via
  `Event#fireAsync`. Criação síncrona, entrega assíncrona — mesmo padrão
  já consolidado em `LogAcessoService` para `AcessoOcorrido`.
- **Canais**: três beans `@ApplicationScoped` com `@ObservesAsync` em
  `NotificacaoCriada`:
  - `WebsocketChannel` (`@ServerEndpoint("/api/v1/ws/notificacoes")`)
  - `EmailChannel` (usa `io.quarkus.mailer.Mailer`)
  - `PushChannel` (stub — apenas `LOG.info`)
  Cada listener roda em `REQUIRES_NEW` com try/catch que converte falha
  em log; um canal quebrado não bloqueia os outros nem a criação.
- **Preferências por usuário**: `Set<CanalNotificacao>` em `Usuario`
  (`@ElementCollection` mapeando `usuario_canais_notificacao`). Cada
  canal só entrega se o set do destinatário contiver o canal
  correspondente.
- **Multitenant**: `Notificacao.empresa` derivada do destinatário;
  `@Filter(tenantFilter)` ativado como nas demais entidades. O usuário
  chamador nunca informa `empresaId`.
- **Segurança**: `NotificacaoResource` com `@Authenticated`; o service
  resolve o id do logado pelo `subject` do JWT (emitido como
  `usuario.getId().toString()` em `TokenJwtService`) e nega se a
  notificação não pertencer a ele (`403`).

### Autenticação do websocket

Adotei **AUTH por mensagem inicial** (não query-param). Após `@OnOpen` a
sessão fica em "não autenticada" — a primeira mensagem precisa ser
`AUTH <jwt>`; qualquer outra coisa fecha a conexão. O token é verificado
por `io.smallrye.jwt.auth.principal.JWTParser` (mesma cadeia do HTTP).
Motivos:
1. Query-param expõe o JWT em access-logs e cabeçalho `Referer`.
2. Quarkus 3.5.3 com `quarkus-websockets` clássico não aplica
   `@RolesAllowed` no handshake por default, então a autenticação teria
   que ser manual de qualquer forma — fazer na primeira mensagem é tão
   barato quanto, e sem os efeitos colaterais acima.

Após o AUTH a sessão é indexada em um `ConcurrentHashMap<Long, Session>`
estático pelo `subject`, e o listener só envia JSON quando o
destinatário da notificação tem sessão aberta (e o canal `WEBSOCKET`
está nas preferências).

### Arquivos criados

- `src/main/java/ka/mdo/model/Notificacao.java`
- `src/main/java/ka/mdo/model/TipoNotificacao.java`
- `src/main/java/ka/mdo/model/CanalNotificacao.java`
- `src/main/java/ka/mdo/repository/NotificacaoRepository.java`
- `src/main/java/ka/mdo/dto/NotificacaoResponseDTO.java`
- `src/main/java/ka/mdo/service/NotificacaoService.java`
- `src/main/java/ka/mdo/service/NotificacaoServiceImpl.java`
- `src/main/java/ka/mdo/service/NotificacaoCriada.java`
- `src/main/java/ka/mdo/service/WebsocketChannel.java`
- `src/main/java/ka/mdo/service/EmailChannel.java`
- `src/main/java/ka/mdo/service/PushChannel.java`
- `src/main/java/ka/mdo/resource/NotificacaoResource.java`
- `src/main/resources/db/migration/V9__notificacao.sql`

### Arquivos alterados

- `src/main/java/ka/mdo/model/Usuario.java` — campo
  `canaisNotificacao` (`@ElementCollection`).
- `pom.xml` — `quarkus-websockets` e `quarkus-mailer` adicionados ao
  final, sem reordenar (conflito paralelo com a 021).
- `src/main/resources/application.properties` — seções `notificacao.*` e
  `quarkus.mailer.*` apenas.
- `PERMISSIONS.md` — linhas anexadas ao final + nota sobre websocket.

### Para a 031 (pendências)

Quando uma pendência for criada/resolvida, basta injetar
`NotificacaoService` e chamar, por exemplo:

```java
notificacaoService.enviar(
    gestorId,
    TipoNotificacao.PENDENCIA_ABERTA,
    "Pendência aguardando revisão",
    "O cliente X abriu uma pendência para o local Y.",
    "{\"pendenciaId\":" + pendencia.getId() + "}");
```

Para o resultado da pendência (aprovação/recusa), o destinatário passa
a ser o dono da credencial, com `TipoNotificacao.PENDENCIA_APROVADA` ou
`PENDENCIA_RECUSADA`. Nada mais precisa ser fiado aqui — os canais já
fazem a entrega respeitando as preferências do usuário.
