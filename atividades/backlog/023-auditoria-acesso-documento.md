---
id: 023
titulo: Auditoria de acesso ao documento completo
prioridade: media
estimativa: P
depende-de: [020, 012]
epico: pessoa
---

## Contexto
**Débito** listado no encerramento da atividade 020. O endpoint
`GET /dados-pessoais/{id}?incluirDocumento=true` devolve o documento em claro
para gestores, mas **não registra** quem leu o quê. Em LGPD/auditoria isso é
um buraco real: vazamento via gestor não deixa rastro.

## Objetivo
Cada leitura de documento completo gera um log dedicado, rastreável por
usuário/data/motivo.

## Critérios de aceitação
- [ ] Entidade nova `LogAcessoDocumento` (`empresa_id`, `usuario_id`,
      `dados_pessoais_id`, `lidoEm`, `motivo`, `ip`).
- [ ] `DadosPessoaisResource.getById(...incluirDocumento=true)` persiste um
      registro a cada chamada.
- [ ] Motivo é parâmetro obrigatório (`?motivo=...`) — sem motivo retorna
      400 (boa prática LGPD).
- [ ] `GET /logs-acesso-documento` (ADMIN_EMPRESA / SUPER_ADMIN) — listagem
      paginada do tenant para auditoria.
- [ ] Migração nova (V16+).
- [ ] Endpoint que retorna o documento em claro é cacheável com `Cache-Control:
      no-store`.
- [ ] PERMISSIONS.md atualizado.

## Notas técnicas
- IP via `@Context HttpServerRequest`.
- Não exposição direta ao cliente: o LogAcessoDocumento é só para gestores.
