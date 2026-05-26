---
id: 004
titulo: Refresh token + revogação de JWT
prioridade: media
estimativa: M
depende-de: [002]
epico: fundacao
---

## Contexto
Hoje o `TokenJwtService` emite JWT com expiração de **200 dias** e o sistema não tem
mecanismo de revogação (logout só descarta o token no cliente). Isso é um risco real:
um token vazado fica válido por meses, e SUPER_ADMIN compromete a instância inteira.

## Objetivo
Reduzir o TTL do access-token, oferecer um endpoint de refresh, e permitir invalidar
tokens (logout efetivo, troca de senha, conta desativada).

## Critérios de aceitação
- [ ] Access-token com TTL curto (sugestão: 15 min) emitido em `POST /auth`.
- [ ] Refresh-token opaco (não-JWT) com TTL longo (ex.: 30 dias) retornado junto.
- [ ] `POST /auth/refresh` aceita o refresh-token e devolve um novo par.
- [ ] `POST /auth/logout` invalida o refresh-token (uso único após login).
- [ ] Tabela `refresh_token` (entidade nova) com `tokenHash`, `usuarioId`, `criadoEm`,
      `expiraEm`, `revogadoEm`, `motivo`. Filtro de tenant.
- [ ] Troca de senha (atividade 005) invalida todos os refresh-tokens do usuário.

## Notas técnicas
- Guardar **hash** (SHA-256) do refresh-token, não o valor cru.
- Cleanup periódico (`@Scheduled`) de tokens expirados/revogados há > N dias.
- Documentar no PERMISSIONS.md.
