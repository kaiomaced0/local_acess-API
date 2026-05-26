---
id: 014
titulo: CRUD de Aparelho
prioridade: alta
estimativa: M
depende-de: [011]
epico: credencial
---

## Contexto
Não existe `AparelhoResource`. Aparelhos hoje só podem ser inseridos direto no
banco — não há como cadastrar/listar/desativar via API. O painel do gestor não
consegue gerenciar os totens.

## Objetivo
CRUD completo de `Aparelho`, isolado por tenant.

## Critérios de aceitação
- [ ] `GET /aparelhos` (ADMIN_EMPRESA / SUPER_ADMIN) — lista paginada com
      filtros por `ativo`, `eventoId`, `localEspecificoId`.
- [ ] `GET /aparelhos/{id}` — detalhe.
- [ ] `POST /aparelhos` — cria com `descricao`, `eventoId` opcional,
      `localEspecificoId` opcional. Empresa vem do JWT.
- [ ] `PUT /aparelhos/{id}` — edita descrição e vínculos (evento/local).
- [ ] `PATCH /aparelhos/{id}/desativar` e `/reativar` — toggle `ativo`.
- [ ] Validação cross-tenant: `eventoId` e `localEspecificoId` devem pertencer
      ao mesmo tenant (403 senão).
- [ ] PERMISSIONS.md atualizado.

## Notas técnicas
- Filtro Hibernate `tenantFilter` já cobre `Aparelho`.
- Preparar entidade para 007 (credencial M2M) — campos `clientId`/`secretHash`
  podem ser adicionados em migração separada.
