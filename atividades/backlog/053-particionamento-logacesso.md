---
id: 053
titulo: Particionamento mensal de LogAcesso
prioridade: baixa
estimativa: G
depende-de: [012]
epico: observabilidade
---

## Contexto
**Débito documentado** em `LogAcesso.java:37` e `LogAcessoService.java:39`:
"TODO futuro: particionar `LogAcesso` por mês se o volume exigir". Hoje a tabela
é monolítica. Em produção, com vários eventos grandes, vira hot spot — listagens
do gestor degradam progressivamente e o backup fica gigante.

## Objetivo
Particionar fisicamente `LogAcesso` por mês (Postgres declarative partitioning),
com routing automático na escrita e poda nas leituras.

## Critérios de aceitação
- [ ] Migração transformando `LogAcesso` em tabela particionada por
      `RANGE (dataHora)`, mensal.
- [ ] Job (`@Scheduled` mensal) que cria a partição do mês seguinte.
- [ ] Job opcional que **detacha** partições antigas além de N meses
      (configurável; default 24) sem dropá-las imediatamente.
- [ ] Migrar dados existentes para a partição certa (script de uma vez).
- [ ] Queries do `LogAcessoService` continuam funcionando sem mudanças
      (delegado ao planner via constraint exclusion).
- [ ] Documentado no README + ADR curto.

## Notas técnicas
- Postgres-only (a abstração entre MariaDB/Postgres deixou de existir desde a
  pivotada para Postgres como banco oficial).
- Hibernate ORM com tabelas particionadas funciona transparentemente.
