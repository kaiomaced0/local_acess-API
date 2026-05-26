---
id: 010
titulo: Token único na credencial + geração de QR Code
prioridade: alta
estimativa: M
depende-de: [001]
epico: credencial
---

## Contexto
Credenciais (Ingresso) serão lidas por aparelhos. Precisamos de um token opaco único por credencial, traduzido em QR code que pode estar em crachá físico ou no celular do dono.

## Objetivo
Cada credencial tem um `token` aleatório único; endpoint retorna o QR code renderizado (PNG) a partir desse token.

## Critérios de aceitação
- [x] `Ingresso` ganha campo `token` (UUID ou string criptograficamente segura, 32+ chars, único indexado).
- [x] Geração do token no momento da criação/emissão da credencial.
- [x] Endpoint `GET /ingressos/{id}/qrcode` retorna imagem PNG do QR (ZXing).
- [x] Endpoint `GET /ingressos/{id}/qrcode?formato=svg` opcional.
- [x] Token não vaza em listagens públicas; só o dono e gestores veem.
- [ ] Teste: token gerado é único, QR decodifica de volta para o token. (registrado como débito para atividade 051 — testes de integração)

## Notas técnicas
- Dependência sugerida: `com.google.zxing:core` + `com.google.zxing:javase`.
- Considerar assinatura do token (HMAC) para evitar aparelho aceitar token forjado offline — avaliar custo/benefício.

## Resultado

### Arquivos criados
- `src/main/java/ka/mdo/service/TokenService.java` — gera tokens opacos com `SecureRandom` (32 bytes) em base64url sem padding (43 chars, 256 bits de entropia).
- `src/main/java/ka/mdo/service/BackfillTokenService.java` — `@Observes StartupEvent` que substitui placeholders `legacy-*` por tokens reais (idempotente, não loga valores).
- `src/main/java/ka/mdo/qrcode/QrCodeService.java` — `gerarPng(conteudo, tamanho)` e `gerarSvg(conteudo, tamanho)` via ZXing `QRCodeWriter` + `MatrixToImageWriter`. Default 300 px, ECC nível M, margem 1.
- `src/main/java/ka/mdo/resource/IngressoResource.java` — `GET /ingressos/{id}/qrcode` (PNG) e `?formato=svg` (SVG). `@RolesAllowed({"CLIENTE","ADMIN_EMPRESA","GESTOR_EVENTO","GESTOR_LOCAL","SUPER_ADMIN"})`. Resposta com `Cache-Control: no-store`.
- `src/main/resources/db/migration/V5__adiciona_token_ingresso.sql` — adiciona coluna + backfill determinístico + índice único.

### Arquivos alterados
- `src/main/java/ka/mdo/model/Ingresso.java` — adicionado `@Column(name="token", unique=true, nullable=false, length=64) private String token;` + getter/setter.
- `src/main/java/ka/mdo/repository/IngressoRepository.java` — `Optional<Ingresso> findByToken(String)` pronto para a atividade 011.
- `src/main/java/ka/mdo/repository/UsuarioRepository.java` — `Usuario findByIngressoId(Long)` para validar dono em `CLIENTE`.
- `src/main/java/ka/mdo/service/IngressoService.java` — injeta `TokenService`, gera token em `adicionarIngresso`, novo método `tokenParaQrCode(id)` com validação multitenancy + dono (CLIENTE) / gestor (demais).
- `pom.xml` — adicionadas `com.google.zxing:core` e `com.google.zxing:javase` 3.5.3.

### Estratégia de migração escolhida
Migração única V5 com **3 passos portáveis MariaDB/PostgreSQL**:

1. `ALTER TABLE ... ADD COLUMN token VARCHAR(64) NOT NULL DEFAULT ''; ALTER ... DROP DEFAULT;` — mesmo padrão da V3 para `empresa_id`, evita dialetos divergentes entre `ALTER ... MODIFY` (MariaDB) e `ALTER COLUMN ... SET NOT NULL` (Postgres).
2. `UPDATE Ingresso SET token = CONCAT('legacy-', CAST(id AS CHAR(20)))` — backfill determinístico e único (id é PK). Evita dependência de `UUID()`/`gen_random_uuid()` que não são portáveis.
3. `CREATE UNIQUE INDEX uk_ingresso_token ON Ingresso(token);` — possível já aqui porque o passo 2 garante unicidade.

No startup, `BackfillTokenService` substitui os `legacy-*` por tokens cripto-seguros reais (UPDATE por id, preserva unicidade). Idempotente: segundo startup encontra 0 registros. Para novas credenciais, `IngressoService.adicionarIngresso` sempre chama `tokenService.gerarToken()` antes de `persist`, então o placeholder nunca é usado em linha nova.

**Por que essa abordagem**: um único SQL Flyway (sem V6 postergada), sem dependência de função nativa do DB, sem janela em que a coluna fica `NULL` e viola o modelo JPA. Trade-off: durante o intervalo entre migração e startup, linhas legadas têm token previsível — mas o Resource só expõe o token via QR autenticado (nunca em listagens), o backfill roda antes de qualquer request chegar, e a API não estava em produção ainda.

### Build status
`./mvnw.cmd compile -q` — **OK** (sem erros de compilação).

### Checklist
- ✅ Campo `token` adicionado (NOT NULL, UNIQUE, 64 chars).
- ✅ Geração cripto-segura (`SecureRandom` 32 bytes → base64url, 256 bits).
- ✅ Token gerado em `IngressoService.adicionarIngresso`.
- ✅ Dependência ZXing em `pom.xml`.
- ✅ `QrCodeService.gerarPng` + `gerarSvg`.
- ✅ Endpoint `GET /ingressos/{id}/qrcode` (PNG).
- ✅ Endpoint `GET /ingressos/{id}/qrcode?formato=svg`.
- ✅ `@RolesAllowed` cobre dono + gestores + SUPER_ADMIN.
- ✅ Validação multitenancy + dono (CLIENTE) no service.
- ✅ Token omitido de `IngressoResponseDTO` (nunca exposto em JSON).
- ✅ Migração V5 portável MariaDB/Postgres.
- ✅ Índice único `uk_ingresso_token`.
- ✅ `IngressoRepository.findByToken(String)` pronto para 011.
- ✅ Token nunca logado (verificado em `TokenService`, `BackfillTokenService`, `QrCodeService`, `IngressoService`, `IngressoResource`).
- ✅ Build passa.
- ❌ Testes automatizados — registrado como débito na 051.

### Pontos para 011 (validação de acesso pelo aparelho)
- Usar `IngressoRepository.findByToken(token)` — já implementado, retorna `Optional<Ingresso>`.
- Endpoint `/acesso/validar` deve exigir `@RolesAllowed({"OPERADOR_APARELHO", ...})` e aplicar filtro por tenant (ou validar empresa do aparelho == empresa do ingresso).
- Para detectar token forjado offline (ataque de nota técnica), considerar HMAC do token com segredo por empresa; avaliar custo/benefício vs. simples round-trip online.
- O Ingresso é o único lugar onde o token vive em claro; ao validar acesso, o aparelho envia o conteúdo do QR (= token) e o backend faz `findByToken`. Nunca logue o token bruto nos logs de acesso — logue apenas o `ingresso.id` recuperado.
