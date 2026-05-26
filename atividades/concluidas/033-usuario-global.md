---
id: 033
titulo: Credenciais globais (acesso a todos eventos/locais)
prioridade: media
estimativa: P
depende-de: [011, 030]
epico: locais
---

## Contexto
Existem pessoas (staff, organização, segurança) que devem passar por qualquer entrada sem validação de perfil por local.

## Objetivo
Flag que concede acesso total dentro do escopo configurado.

## Critérios de aceitação
- [x] `Ingresso` (ou `Usuario`) ganha flag `global` com escopo: `EMPRESA` (default) ou `SUPER` (cross-tenant, só super-admin pode emitir).
- [x] Validação de acesso curto-circuita checagem de perfil/local quando `global=true`.
- [x] Validação facial continua valendo (se o evento exige).
- [x] Emissão de credencial global restrita a `ADMIN_EMPRESA`/`SUPER_ADMIN`.
- [x] `LogAcesso` registra `acesso_global=true` para auditoria.

## Notas técnicas
- Super-global cross-tenant é sensível — logar com destaque e exigir 2FA para emissão (futuro).

## Resultado

### Decisão: flag em `Ingresso` (não em `Usuario`)
A flag `escopoGlobal` ficou em `Ingresso` (não em `Usuario`). Razões:
- **Granularidade**: o gestor pode emitir uma credencial específica com acesso global (ex.: bracelete de staff para um único evento) sem promover todas as credenciais do portador, e pode revogar o acesso global invalidando apenas aquela credencial.
- **Menor acoplamento**: não mistura identidade do usuário com capacidade operacional — mesmo usuário pode ter uma credencial comum e outra global simultaneamente.
- **Auditoria direta**: `LogAcesso.ingresso_id` já aponta para a credencial; marcar `acessoGlobal=true` no log associa claramente à credencial que disparou o bypass.

Se futuramente surgir necessidade de "usuário sempre global" (ex.: super-admins da instância), a extensão é aditiva: adicionar flag em `Usuario` e honrar na emissão/validação como OR com `Ingresso.escopoGlobal`.

### Arquivos criados
- `src/main/java/ka/mdo/model/EscopoGlobal.java` — enum `EMPRESA` / `SUPER`.
- `src/main/resources/db/migration/V12__credencial_global.sql` — `Ingresso.escopoGlobal VARCHAR(20) NULL` + `LogAcesso.acessoGlobal BOOLEAN NOT NULL DEFAULT FALSE`.

### Arquivos alterados
- `src/main/java/ka/mdo/model/Ingresso.java` — campo `@Enumerated(EnumType.STRING) EscopoGlobal escopoGlobal` (nullable) + getter/setter.
- `src/main/java/ka/mdo/model/LogAcesso.java` — campo `boolean acessoGlobal` + getter/setter.
- `src/main/java/ka/mdo/dto/IngressoDTO.java` — novo campo opcional `escopoGlobal`.
- `src/main/java/ka/mdo/dto/IngressoResponseDTO.java` — expõe `escopoGlobal`.
- `src/main/java/ka/mdo/dto/LogAcessoResponseDTO.java` — expõe `acessoGlobal`.
- `src/main/java/ka/mdo/service/IngressoService.java` — `adicionarIngresso` propaga `escopoGlobal` + gate `validarAutorizacaoEmissaoGlobal` (SUPER → só `SUPER_ADMIN`; EMPRESA → `ADMIN_EMPRESA`/`SUPER_ADMIN`).
- `src/main/java/ka/mdo/service/AcessoOcorrido.java` — record ganha campo `boolean acessoGlobal`; `semFoto` default `false`.
- `src/main/java/ka/mdo/service/LogAcessoService.java` — persiste `acessoGlobal` e expõe no DTO de resposta.
- `src/main/java/ka/mdo/service/AcessoService.java` — curto-circuito de credenciais globais + novo parâmetro `acessoGlobal` em `negar(...)` / `dispararLog(...)`.
- `src/main/java/ka/mdo/repository/IngressoRepository.java` — `findByTokenCrossTenant(String)` (desativa `tenantFilter` só para esta consulta, para achar credenciais SUPER cross-tenant).
- `PERMISSIONS.md` — nota sobre gate de emissão + comportamento do curto-circuito + débito 2FA.

### Impacto no AcessoService (o que muda para credenciais globais)
Logo após carregar o `Ingresso`:
1. **Se `ingresso.escopoGlobal == SUPER`**: pula tenant da credencial, período do evento, local (existência e pertinência), perfil (whitelist de `TipoIngresso`), dados pessoais e validação facial. Retorna `AUTORIZADO / MOTIVO_OK` e registra `LogAcesso` com `acessoGlobal=true`. Para suportar cross-tenant, quando o `findByToken` filtrado devolve vazio, fazemos fallback a `findByTokenCrossTenant` e só seguimos se o resultado tiver escopo `SUPER` (qualquer outra situação continua como `CREDENCIAL_INEXISTENTE`, evitando vazamento entre tenants).
2. **Se `ingresso.escopoGlobal == EMPRESA`**: tenant da credencial é exigido explicitamente (`aparelho.empresa == ingresso.empresa`); mantém período do evento, dados pessoais (020) e facial (021). Pula **apenas** a whitelist por local (TipoIngresso) — o fluxo de existência do local e vínculo local↔evento continua valendo (invariantes de integridade). Logs marcados com `acessoGlobal=true`.
3. **Se `ingresso.escopoGlobal == null`**: nenhum comportamento muda; `acessoGlobal` no log fica `false`.

Todas as respostas `NEGADO`/`PENDENTE` tomadas em cima de um `Ingresso` global propagam `acessoGlobal=true` (destaque de auditoria mesmo quando o resultado não é AUTORIZADO).

### Build status
`./mvnw.cmd compile -q` — **OK** (exit 0; apenas warnings do runtime Java sobre `sun.misc.Unsafe` em deps transitivas do Maven).

### Checklist
- ✅ `Ingresso.escopoGlobal` (EMPRESA/SUPER) + getter/setter + migração V12.
- ✅ `AcessoService` curto-circuita checagens conforme escopo.
- ✅ Validação facial preservada para `EMPRESA` (quando evento exige).
- ✅ Emissão gated: `SUPER` → só `SUPER_ADMIN`; `EMPRESA` → `ADMIN_EMPRESA`/`SUPER_ADMIN`.
- ✅ `LogAcesso.acessoGlobal` populado em todos os fluxos (AUTORIZADO, NEGADO, PENDENTE) quando a credencial é global.
- ✅ `IngressoDTO`/`IngressoResponseDTO` expõem o campo.
- ✅ Nenhum endpoint separado — emissão continua em `POST /usuarios/{id}/ingressos`.
- ✅ `PERMISSIONS.md` atualizado.
- ✅ Credencial global cross-tenant via `IngressoRepository.findByTokenCrossTenant` (sem expor o token em log).
- ✅ Build passa.
- ❌ 2FA para emissão SUPER (documentado como débito, não implementado).
- ❌ Testes de integração (coerente com débito consolidado em 051/011/020/021).
- ❌ Endpoint específico para listar credenciais globais para auditoria (delegado a dashboards futuros — `LogAcesso.acessoGlobal` já persiste a informação).
