---
id: 054
titulo: Paginação e filtros nas listagens
prioridade: media
estimativa: M
depende-de: [001]
epico: observabilidade
---

## Contexto
Endpoints como `GET /usuarios`, `GET /eventos`, `GET /logs-acesso` retornam
`List<DTO>` sem paginação. Em produção, com milhares de registros, o response
explode em payload e tempo de query. Alguns endpoints (`/pendencias`,
`/metricas/.../entradas`) já fazem paginação ad-hoc — a convenção precisa virar
padrão.

## Objetivo
Padronizar paginação + filtros em todas as listagens do projeto.

## Critérios de aceitação
- [ ] Convenção: `?pagina=0&tamanho=20` (defaults) e `?ordem=campo,asc`
      consistente em **todas** as listagens.
- [ ] Resposta padrão: `Page<T>` com `{ conteudo, pagina, tamanho, total,
      totalPaginas }`.
- [ ] Limite máximo de `tamanho` aplicado server-side (default 200).
- [ ] Filtros aplicáveis documentados via `@QueryParam` por endpoint
      (ex.: `/usuarios?perfil=CLIENTE&nome=joao`).
- [ ] Helper genérico para Panache (`PageHelper.aplicar`).
- [ ] OpenAPI documenta os parâmetros.

## Notas técnicas
- Panache já tem `Page.of(p, t)`.
- Manter compat: `GET /eventos` sem parâmetros continua respondendo, default
  primeira página.
