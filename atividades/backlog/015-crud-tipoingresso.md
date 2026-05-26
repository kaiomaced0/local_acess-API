---
id: 015
titulo: CRUD de TipoIngresso
prioridade: alta
estimativa: P
depende-de: [001, 030]
epico: credencial
---

## Contexto
`TipoIngresso` é referenciado em `Ingresso`, na whitelist por `EspacoEvento`
(atividade 030) e na emissão de credencial — mas **não existe resource REST**.
Tipos só são inseridos via banco. Sem isso o admin não consegue configurar
"Pista", "VIP", "Camarote", etc.

## Objetivo
CRUD completo de `TipoIngresso`, isolado por tenant.

## Critérios de aceitação
- [ ] `GET /tipos-ingresso` (ADMIN_EMPRESA / GESTOR_EVENTO / SUPER_ADMIN) —
      lista paginada do tenant.
- [ ] `GET /tipos-ingresso/{id}`.
- [ ] `POST /tipos-ingresso` — cria com `nome` (único por empresa).
- [ ] `PUT /tipos-ingresso/{id}` — edita `nome`.
- [ ] `DELETE /tipos-ingresso/{id}` — soft-delete (`ativo=false`); falha se
      houver `Ingresso` ativo referenciando.
- [ ] Endpoint para vincular a locais reusa `/espacos-evento/{id}/autorizacoes`
      (atividade 030).
- [ ] PERMISSIONS.md atualizado.

## Notas técnicas
- Constraint unique `(empresa_id, nome)` via migração.
