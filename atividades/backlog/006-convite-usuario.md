---
id: 006
titulo: Convite de usuário por email
prioridade: media
estimativa: M
depende-de: [002, 032]
epico: fundacao
---

## Contexto
`POST /usuarios` exige que o admin já saiba a senha do convidado — força senha
temporária comunicada por canal externo. O padrão de produto é o convite: admin
informa email + perfil, sistema envia link, convidado define a própria senha.

## Objetivo
Fluxo de convite onde o admin não escolhe senha; o convidado a define ao aceitar.

## Critérios de aceitação
- [ ] `POST /usuarios/convidar` (ADMIN_EMPRESA / SUPER_ADMIN) recebe
      `{ email, nome, cpf, perfis }` e cria `Usuario` em estado
      `aguardandoAceite=true` (sem senha persistida).
- [ ] Tabela `convite_token` (`tokenHash`, `usuarioId`, `expiraEm`, `usadoEm`),
      TTL configurável (default 72h).
- [ ] Email com link `${frontend.url}/aceitar-convite?token=...`.
- [ ] `POST /usuarios/aceitar-convite` recebe `{ token, senha }`, marca
      `aguardandoAceite=false` e persiste hash.
- [ ] Reenvio: `POST /usuarios/{id}/reenviar-convite` invalida convites
      pendentes do mesmo usuário e gera um novo.
- [ ] Usuário com `aguardandoAceite=true` não consegue logar (falha em
      `byLoginAndSenha`).

## Notas técnicas
- Coluna `usuario.aguardando_aceite` (boolean, default false) — migração nova.
- Aproveitar `NotificacaoService` + canal email.
- Documentar no PERMISSIONS.md.
