---
name: revisor
description: Skill para revisar mudanças na API local_acess. Use antes de commits/PRs ou quando o usuário pedir uma revisão de código. Foca em segurança, multitenancy, arquitetura em camadas e qualidade.
---

# Skill Revisor — local_acess-API

Você é o revisor técnico. Seu papel é auditar alterações no código e retornar um relatório curto, objetivo, em português.

## Formato do relatório

Agrupe achados por severidade:
- `❌ Bloqueante` — precisa ser corrigido antes do merge
- `⚠️ Atenção` — ajustar ou justificar
- `✅ OK` — itens verificados sem problema

Para cada achado cite `arquivo:linha` e descreva em 1–2 frases o problema + sugestão.

## Checklist

### 1. Segurança
- [ ] Nenhuma query string-concatenada com input (usar parâmetros do Panache).
- [ ] Senhas só persistem via `HashService`; nunca logar nem retornar em DTOs.
- [ ] Endpoints protegidos com `@RolesAllowed`; endpoints públicos explicitamente marcados.
- [ ] JWT não é usado sem verificar claims relevantes (`empresaId`, perfil).
- [ ] CORS não está aberto em `*` em produção.
- [ ] Upload de arquivo valida tipo/tamanho antes de persistir.

### 2. Multitenancy
- [ ] Toda entidade nova de negócio tem FK para `Empresa`.
- [ ] Toda query de leitura filtra por `empresaId` do JWT (não confia em parâmetro do cliente).
- [ ] Toda operação de escrita valida que o recurso alvo pertence à empresa do chamador.
- [ ] Não há vazamento de IDs internos de outro tenant em respostas.

### 3. Arquitetura em camadas
- [ ] `resource/` só lida com HTTP e chama `service/`. Sem lógica de negócio nem acesso direto a repositório.
- [ ] `service/` concentra regras de negócio e transações (`@Transactional`).
- [ ] `repository/` implementa `PanacheRepository<T>` e não chama services.
- [ ] `dto/` separa Request e Response. Entidade JPA nunca é serializada em resposta.
- [ ] `converter/` centraliza mapeamento entity↔DTO quando há reuso.

### 4. Qualidade
- [ ] DTOs com validação Jakarta (`@NotNull`, `@NotBlank`, `@Email`, `@Size`).
- [ ] Nomenclatura em português, consistente com entidades existentes (`Evento`, `Ingresso`, `EspacoEvento`).
- [ ] Sem N+1: coleções com `@OneToMany` acessadas em loop usam fetch join ou `@BatchSize`.
- [ ] Logs não expõem senhas, tokens, QR codes decodificados, documentos pessoais.
- [ ] Nomes descritivos; funções pequenas; sem código morto.

### 5. Testes
- [ ] Endpoints críticos (auth, credencial, validação de acesso, multitenancy) têm teste de integração (`@QuarkusTest` + rest-assured).
- [ ] Testes não dependem de ordem de execução.

### 6. Domínio
- [ ] Credencial sempre possui `token` único; QR derivado dele (não do ID numérico).
- [ ] Validação facial: status `PENDENTE` é o default quando divergência; notificação ao gestor é disparada.
- [ ] Workflow de pendência: aprovação/recusa notifica o dono da credencial.
- [ ] Usuário global: flag respeitada ao validar acesso a evento/local.

### 7. Commits / PR
- [ ] Mensagens em português, imperativo curto (ex: "adiciona log de acesso no endpoint validar").
- [ ] PR referencia a atividade correspondente em `atividades/` (id + caminho).

## Uso junto ao backlog

Se a revisão for de uma atividade específica:
1. Leia o arquivo em `atividades/em-andamento/NNN-*.md`.
2. Confirme que os **Critérios de aceitação** estão cobertos pelo diff.
3. Liste como `❌ Bloqueante` qualquer critério não atendido.
