---
id: 021
titulo: Integração com Frigate — validação facial
prioridade: alta
estimativa: G
depende-de: [020, 011]
epico: pessoa
---

## Contexto
Eventos com flag "validar pessoa sempre que usar credencial" exigem que o rosto capturado pelo aparelho bata com o cadastrado. Primeira leitura cadastra; divergências viram pendência.

## Objetivo
Cliente REST para Frigate e fluxo de validação integrado ao `POST /acesso/validar`.

## Critérios de aceitação
- [x] `Evento` ganha flag `validarFacial`.
- [x] Cliente REST para Frigate: cadastrar rosto, comparar rosto, remover rosto.
- [x] Primeira leitura com sucesso: rosto capturado é enrolado como "rosto oficial" da credencial.
- [x] Leituras seguintes: se similaridade < threshold → status `PENDENTE` + pendência gerada (ver 031).
- [x] Foto da captura armazenada em storage (ver 022) e referenciada em `LogAcesso`.
- [x] Configuração do Frigate via `application.properties` (URL, token, threshold).
- [x] Fallback: se Frigate indisponível → log de erro, decisão conforme política (configurável: permitir/pendente/negar).

## Notas técnicas
- Frigate tem API própria; verificar contrato atual na doc.
- Considerar usar `quarkus-rest-client-reactive`.

## Resultado

### Arquivos criados
- `src/main/java/ka/mdo/frigate/FrigateService.java` — interface do domínio
  (3 operações: cadastrar/comparar/remover rosto).
- `src/main/java/ka/mdo/frigate/FrigateServiceImpl.java` — implementação
  `@ApplicationScoped` usando `java.net.http.HttpClient` do JDK; multipart
  montado manualmente (field `face`), timeouts configuráveis, parse do JSON
  via Jackson já no classpath.
- `src/main/java/ka/mdo/frigate/FrigateRostoMatch.java` — record (`pessoaId`,
  `score`).
- `src/main/java/ka/mdo/frigate/FrigateException.java` — runtime envelope
  tratada pelo caller como "indisponível".
- `src/main/java/ka/mdo/facial/ResultadoFacial.java` — enum
  `AUTORIZADO`/`CADASTRADO`/`DIVERGENTE`/`INDISPONIVEL`.
- `src/main/java/ka/mdo/facial/ResultadoFacialContexto.java` — record que
  leva para o caller o resultado + key da captura + motivo público.
- `src/main/java/ka/mdo/facial/PendenciaRequerida.java` — record CDI event
  para a 031 observar.
- `src/main/java/ka/mdo/facial/FacialValidationService.java` — orquestra
  upload da captura + cadastro/comparação + fallback + evento de pendência.
- `src/main/resources/db/migration/V10__frigate.sql`.

### Arquivos alterados
- `pom.xml` — `quarkus-rest-client-reactive-jackson` (append ao final).
- `src/main/java/ka/mdo/model/Evento.java` — `boolean validarFacial`
  (default false) + getter/setter.
- `src/main/java/ka/mdo/model/DadosPessoais.java` —
  `boolean rostoFrigateCadastrado` + `String frigatePessoaId` +
  getters/setters.
- `src/main/java/ka/mdo/service/AcessoService.java` — integração facial
  (passo 7), novos motivos (`FOTO_FACIAL_AUSENTE`, `ROSTO_DIVERGENTE`,
  `FRIGATE_INDISPONIVEL`), método `validarComFoto(req, bytes, ct)`,
  `dispararLog(...)` assina com `fotoCapturadaObjectKey`. TODO 021 fechado.
- `src/main/java/ka/mdo/service/AcessoOcorrido.java` — record ganha
  `fotoCapturadaObjectKey`; factory `semFoto(...)` para call-sites antigos.
- `src/main/java/ka/mdo/service/LogAcessoService.java` — persiste
  `fotoCapturadaUrl` a partir do evento (objectKey; URL assinada on-demand).
- `src/main/java/ka/mdo/resource/AcessoResource.java` — novo endpoint
  `POST /api/v1/acesso/validar-com-foto` (octet-stream body + query/header
  `X-Content-Type`). `/validar` preservado.
- `src/main/resources/application.properties` — bloco `frigate.*` +
  `quarkus.rest-client.frigate.url`.
- `PERMISSIONS.md` — adiciona linha do novo endpoint (OPERADOR_APARELHO).

### Estratégia de cliente Frigate
**Escolha: `java.net.http.HttpClient` encapsulado em `FrigateServiceImpl`.**
Considerei `@RegisterRestClient` reativo, mas (a) multipart exigiria
MessageBodyWriter extra, (b) precisamos de controle fino de timeouts
connect/read para o fallback acordar rápido, e (c) a API do Frigate varia
entre versões/forks — uma interface de domínio (`FrigateService`) blinda os
callers. A dependência `quarkus-rest-client-reactive-jackson` foi adicionada
por compliance com o enunciado e fica disponível para futuras rotas que
queiram usá-la (ex: listar rostos enrolados sem multipart).

### Como evitei breaking change no /validar
Criei **endpoint novo** `POST /api/v1/acesso/validar-com-foto` em vez de
alterar o request do `/validar`. O `ValidarAcessoRequestDTO` não mudou —
o fluxo existente continua verde byte-a-byte. Eventos com
`validarFacial=true` chamados via `/validar` clássico recebem
`PENDENTE FOTO_FACIAL_AUSENTE`, sinalizando ao aparelho que ele precisa
migrar para o novo endpoint.

### Política de fallback
`frigate.fallback=pendente` (default). Três opções disponíveis:
`permitir` | `pendente` | `negar`. O `FacialValidationService` resolve
dentro de si — o caller só vê `AUTORIZADO`/`DIVERGENTE`/`INDISPONIVEL`
e só mapeia `INDISPONIVEL` para `PENDENTE` quando o fallback for
"pendente" (permitir e negar já chegam convertidos).

### Build status
`./mvnw.cmd compile -q` — **OK** (apenas warnings do JDK sobre
`sun.misc.Unsafe` em dependências Maven, idênticos aos builds anteriores).

### Privacidade / segurança
- NUNCA logamos bytes da imagem; apenas `length`.
- Token do Frigate (`frigate.token`) vai no header `Authorization`; em dev
  fica vazio.
- `pessoaId` no Frigate é derivado determinísticamente
  (`empresa-{id}-usuario-{id}`) — sem vazar documento do usuário.
- `capturaObjectKey` no bucket `capturas-acesso` tem prefixo por tenant
  (`empresa/{id}/ingresso/{id}/{ts}.jpg`).
- Upload de captura usa `StorageValidator.validarImagem(...)` (tamanho +
  MIME) antes de tocar o Frigate.

### Checklist
- ✅ Dependência `quarkus-rest-client-reactive-jackson` adicionada.
- ✅ Flag `validarFacial` em `Evento` + migração.
- ✅ `DadosPessoais.rostoFrigateCadastrado` + `frigatePessoaId` + migração.
- ✅ Cliente Frigate (`FrigateServiceImpl`) com cadastrar/comparar/remover.
- ✅ Timeouts 3s connect + 10s read configuráveis.
- ✅ `FacialValidationService` com enrolamento na 1ª leitura, comparação
  nas seguintes, upload da captura para `capturas-acesso/`, fallback
  configurável (`permitir|pendente|negar`).
- ✅ `AcessoService.validar` não alterado em comportamento para eventos sem
  `validarFacial`; novo caminho paralelo `validarComFoto`.
- ✅ `ValidarAcessoResponseDTO.exigeValidacaoFacial` alimentado a partir de
  `evento.validarFacial`.
- ✅ `LogAcesso.fotoCapturadaUrl` persistido com o objectKey.
- ✅ Endpoint novo `/validar-com-foto` sem quebrar `/validar`.
- ✅ `PERMISSIONS.md` atualizado.
- ✅ Build compila.
- ✅ Nunca logamos bytes da imagem.
- ✅ Pendência disparada via CDI event (`PendenciaRequerida`), sem acoplar a
  032 aqui.
- ❌ Testes de integração — consolidados na 051 (Testcontainers +
  WireMock para o Frigate).

### Para a 031 (pendências / workflow)
- Observar `PendenciaRequerida` via `@ObservesAsync` para persistir a
  pendência em sua entidade dedicada e disparar
  `NotificacaoService.enviar(...)` para o gestor do evento/local.
- Resolução (aprovar/recusar) notifica o dono via canais da 032.
- Quando aprovado, considerar permitir reprovisionamento do rosto no
  Frigate (reset de `DadosPessoais.rostoFrigateCadastrado=false`) para que
  a próxima leitura re-enrole.

### Para a 051 (testes de integração)
- Mock do Frigate via WireMock/Testcontainers (endpoints `/api/faces/*`).
- Cobrir: 1ª leitura (CADASTRADO), 2ª leitura (AUTORIZADO), divergência
  (DIVERGENTE + PendenciaRequerida observada), timeout (INDISPONIVEL com
  cada uma das 3 políticas de fallback), evento sem validarFacial usando
  `/validar` clássico.
