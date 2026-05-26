---
id: 007
titulo: Autenticação máquina-a-máquina para aparelhos
prioridade: baixa
estimativa: G
depende-de: [002, 014]
epico: fundacao
---

## Contexto
Hoje os totens/leitores logam com email/senha de um usuário real com perfil
`OPERADOR_APARELHO`. É frágil: senha de humano em um device de campo, sem rotação,
sem identificação clara do device em auditoria, e cada totem usa o mesmo login.

## Objetivo
Cada `Aparelho` ganha credencial própria (clientId + clientSecret) com token de
acesso de curto prazo. Auditoria identifica QUAL aparelho fez a leitura, não qual
operador genérico.

## Critérios de aceitação
- [ ] `Aparelho` ganha colunas `clientId` (único) e `clientSecretHash`.
- [ ] `POST /auth/aparelho` aceita `{ clientId, clientSecret }` e devolve JWT
      com `groups=[OPERADOR_APARELHO]`, claim `aparelhoId` (não `usuarioId`).
- [ ] `AcessoService.executarValidacao` passa a casar o JWT contra
      `aparelho.id` via claim `aparelhoId` em vez de só conferir empresa.
- [ ] Rotação: `POST /aparelhos/{id}/rotacionar-secret` (ADMIN_EMPRESA) gera
      novo secret e invalida o anterior; secret é exibido **uma única vez**.
- [ ] CRUD de aparelho (atividade 014) inclui geração inicial do secret.
- [ ] `LogAcesso` continua referenciando `aparelho_id` (já existe).
- [ ] Migração de transição: aparelhos antigos continuam funcionando com o JWT
      do operador humano durante um período de deprecação.

## Notas técnicas
- Secret em claro só no payload de resposta; persistir hash.
- Considerar mTLS no futuro (out of scope desta atividade).
