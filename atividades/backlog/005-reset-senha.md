---
id: 005
titulo: Reset de senha (esqueci minha senha)
prioridade: media
estimativa: M
depende-de: [002, 032]
epico: fundacao
---

## Contexto
Não há fluxo de recuperação de senha. Usuário que esquece está bloqueado — só com
acesso direto ao banco para resetar o hash. Em produção isso vira ticket para o TI.

## Objetivo
Fluxo padrão de reset por email: solicitar → email com link contendo token único
→ definir nova senha.

## Critérios de aceitação
- [ ] `POST /auth/esqueci-senha` recebe `{ email }`, dispara email se existir,
      mas **sempre** responde 204 (não vaza enumeração de contas).
- [ ] Tabela `reset_senha_token` (`tokenHash`, `usuarioId`, `expiraEm`, `usadoEm`),
      TTL curto (15 min), uso único.
- [ ] `POST /auth/redefinir-senha` recebe `{ token, novaSenha }` e atualiza o hash
      via `HashService.getHashSenha`; invalida o token usado.
- [ ] Email usa `NotificacaoService` (template novo `TipoNotificacao.RESET_SENHA`)
      com link `${frontend.url}/redefinir-senha?token=...`.
- [ ] Validação de força mínima da nova senha (≥ 8 caracteres) no DTO.
- [ ] Invalida refresh-tokens do usuário ao redefinir (depende de 004).

## Notas técnicas
- Adicionar `frontend.url` em `application.properties` para montar o link.
- Conferir LGPD: log do email tem que ser redigido em `LogSanitizer`.
