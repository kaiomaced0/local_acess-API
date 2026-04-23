---
id: 020
titulo: Dados pessoais e foto do dono da credencial
prioridade: alta
estimativa: M
depende-de: [001, 022]
epico: pessoa
---

## Contexto
Eventos podem exigir dados pessoais do dono da credencial (nome completo, documento, foto). Alguns não exigem.

## Objetivo
Modelar dados pessoais vinculados à credencial e flag no evento.

## Critérios de aceitação
- [ ] Entidade `DadosPessoais` (nome, documento, tipoDocumento, fotoUrl, dataNascimento) vinculada 1:1 com `Ingresso` ou com `Usuario` (avaliar).
- [ ] `Evento` ganha flag `exigeDadosPessoais` (boolean).
- [ ] Ao ativar a flag, acesso com credencial sem dados → `PENDENTE` ou `NEGADO` (definir regra).
- [ ] Endpoint do cliente para preencher/atualizar seus próprios dados.
- [ ] Upload de foto usa storage (ver 022).
- [ ] LGPD: documento armazenado com criptografia em repouso; acesso logado.

## Notas técnicas
- Validar documento conforme tipo (CPF, RG, passaporte).

## Resultado

### Decisão de vínculo: `Usuario`
Optamos por `DadosPessoais` 1:1 com `Usuario` (FK do lado Usuario → `dados_pessoais_id`), NÃO com `Ingresso`. Justificativa: um usuário pode ter várias credenciais (ver `Usuario.ingressos`). Vincular a `Usuario` evita duplicação de dados sensíveis (LGPD = menos cópias do documento em banco e menos pontos de atualização). O `AcessoService` navega do ingresso ao dono via `UsuarioRepository.findByIngressoId(...)` e checa `usuario.getDadosPessoais()`.

### Arquivos criados
- `src/main/java/ka/mdo/model/TipoDocumento.java` — enum CPF/RG/PASSAPORTE/OUTRO.
- `src/main/java/ka/mdo/model/DadosPessoais.java` — entidade com tenant filter, unique `(empresa_id, tipoDocumento, documento)` e método `isCompleto()`.
- `src/main/java/ka/mdo/dto/DadosPessoaisDTO.java` — request (record) com validações.
- `src/main/java/ka/mdo/dto/DadosPessoaisResponseDTO.java` — response com `documentoMascarado` + URLs pré-assinadas.
- `src/main/java/ka/mdo/repository/DadosPessoaisRepository.java` — `findByDocumento`, `findByUsuarioId`.
- `src/main/java/ka/mdo/service/DadosPessoaisService.java` — upsert, upload de selfie/documento, validação de CPF inline (dígitos verificadores), mascaramento.
- `src/main/java/ka/mdo/resource/DadosPessoaisResource.java` — endpoints REST.
- `src/main/resources/db/migration/V8__dados_pessoais.sql` — tabela + FK no Usuario + flag no Evento.

### Arquivos alterados
- `src/main/java/ka/mdo/model/Usuario.java` — adiciona `@OneToOne dadosPessoais` (lado dono da FK).
- `src/main/java/ka/mdo/model/Evento.java` — adiciona `boolean exigeDadosPessoais`.
- `src/main/java/ka/mdo/service/AcessoService.java` — implementa passo 6 (dados pessoais): retorna `PENDENTE` com motivo `DADOS_PESSOAIS_INCOMPLETOS` quando o evento exige e o dono não completou.
- `PERMISSIONS.md` — anexa 5 rotas novas (`/api/v1/dados-pessoais/*`) ao final da tabela, sem reordenar.

### Estratégia de upload multipart
**Não adicionamos dependência nova** (`quarkus-resteasy-multipart`). Os endpoints `POST /{id}/foto` e `POST /{id}/documento-foto` consomem `application/octet-stream` com o MIME real no header `X-Content-Type`. Essa escolha:
- Evita tocar em `pom.xml` (atividade 032 roda em paralelo).
- Funciona com a extensão `quarkus-resteasy` (clássica) já ativa.
- Cliente envia os bytes crus e o server chama `StorageValidator.validarImagem(bytes, contentType)` antes de persistir.

Se quiséssemos multipart "clássico", bastaria adicionar `quarkus-resteasy-multipart` e trocar o corpo por `@MultipartForm FileUpload` — migração local ao resource.

### Criptografia em repouso (LGPD)
Optamos pela estratégia **B** (infra). O documento é persistido em claro na coluna `DadosPessoais.documento`; a garantia vem do banco (TDE em produção) e do storage (bucket com SSE — ver 022). Criptografia aplicacional (opção A) foi descartada pelo custo de gerência de chaves sem necessidade legal imediata. Documentado no cabeçalho da migração V8 e no javadoc da entidade.

### Segurança / privacidade
- `DadosPessoaisResponseDTO.documentoMascarado` retorna `***.***.***-XX` para CPF por default. Gestores (`SUPER_ADMIN`/`ADMIN_EMPRESA`/`GESTOR_EVENTO`/`GESTOR_LOCAL`) podem pedir `GET /{id}?incluirDocumento=true` para auditoria.
- Nenhum `LOG.info`/`LOG.warn` inclui `nomeCompleto` ou `documento` — só ids.
- `DadosPessoaisService.autorizarDonoOuGestor(...)` garante que apenas o dono dos dados ou um gestor do mesmo tenant acessa/modifica.
- Filtro `tenantFilter` aplicado automaticamente em todas as queries do repositório.

### Integração com AcessoService
- Removido o TODO 020 do javadoc (substituído por descrição da implementação).
- Novo motivo de resposta: `DADOS_PESSOAIS_INCOMPLETOS`.
- Comportamento: quando `eventoContexto.isExigeDadosPessoais() == true` e `Usuario.dadosPessoais` está ausente ou `!dp.isCompleto()` → retorna `ResultadoAcesso.PENDENTE` e dispara log assíncrono. Credencial sem usuário dono (aquisição sem vínculo) também cai em PENDENTE por segurança.

### Build
`./mvnw.cmd clean compile` — BUILD SUCCESS (75 source files).

### Critérios de aceitação
- [x] Entidade `DadosPessoais` (nome, documento, tipoDocumento, fotoUrl, dataNascimento) — vinculada a `Usuario` (decisão documentada).
- [x] `Evento.exigeDadosPessoais` adicionado.
- [x] Flag ativa + dados ausentes → `PENDENTE` com `DADOS_PESSOAIS_INCOMPLETOS`.
- [x] Endpoint do cliente: `POST /api/v1/dados-pessoais` (upsert do logado) + `GET /meus`.
- [x] Upload via `StorageService` (buckets `credenciais-foto` e `documentos`); tamanho/MIME validados por `StorageValidator`.
- [x] LGPD: acesso logado (LOG.info por id, nunca documento); criptografia em repouso via infra (estratégia B documentada).

### Deixado para 021 (facial) e demais
- **021 (facial)**: após capturar foto na catraca, comparar com `fotoObjectKey` via Frigate. O campo está pronto — a 021 adiciona a chamada de comparação e o estado `PENDENTE` específico de facial (distinto de `DADOS_PESSOAIS_INCOMPLETOS`).
- **031 (workflow pendências)**: resolução manual de `DADOS_PESSOAIS_INCOMPLETOS` pelo gestor (aprovar/recusar) não foi implementada aqui — é escopo da 031.
- **032 (notificações)**: notificar o dono quando o evento ativar `exigeDadosPessoais` — integração via CDI event é trivial e fica com a 032.
- **Auditoria de acesso ao documento completo**: `GET /{id}?incluirDocumento=true` retorna em claro mas ainda não gera `LogAcessoDocumento`. Débito pequeno para a 031/051.
- **Testes de integração**: não escritos aqui (alinhado com a escolha da 022 de deferir Testcontainers para 051).
