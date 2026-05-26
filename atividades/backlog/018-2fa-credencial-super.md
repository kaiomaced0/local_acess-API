---
id: 018
titulo: 2FA na emissão de credencial SUPER
prioridade: baixa
estimativa: M
depende-de: [033, 004]
epico: credencial
---

## Contexto
**Débito técnico** documentado na atividade 033: a emissão de credencial
`EscopoGlobal.SUPER` é cross-tenant — um SUPER_ADMIN comprometido emite uma
credencial que abre **qualquer evento de qualquer empresa**. Hoje o único gate é
`@RolesAllowed("SUPER_ADMIN")`. Justifica exigir segundo fator.

## Objetivo
Bloquear `escopoGlobal=SUPER` atrás de uma confirmação adicional de identidade
do operador no momento da emissão.

## Critérios de aceitação
- [ ] Mecanismo de 2FA TOTP (RFC 6238) opt-in por usuário.
- [ ] `POST /usuario-logado/2fa/setup` retorna `secret` base32 + QR Code (otpauth://).
- [ ] `POST /usuario-logado/2fa/confirmar` recebe `{ codigo }` e ativa o 2FA.
- [ ] `IngressoService.adicionarIngresso`: quando `escopoGlobal=SUPER`, exige
      header `X-2FA-Code` válido. Sem código ou inválido ⇒ 401/403 com motivo.
- [ ] Cada código TOTP só pode ser usado uma vez (anti-replay, janela curta).
- [ ] `LogAcesso` (ou log dedicado de auditoria de admin) registra emissões
      SUPER com `usuarioId` que confirmou o 2FA.

## Notas técnicas
- Lib: `dev.samstevens.totp` ou `com.j256.two-factor-auth` — leves, sem deps
  pesadas.
- `Usuario.totpSecret` (criptografado em rest se possível) — migração nova.
- 2FA opt-in: outras emissões (EMPRESA, normal) seguem como hoje.
