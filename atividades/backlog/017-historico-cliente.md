---
id: 017
titulo: Histórico do cliente (minhas passagens)
prioridade: media
estimativa: P
depende-de: [012]
epico: credencial
---

## Contexto
O `LogAcesso` (atividade 012) grava cada leitura, mas o CLIENTE não tem visão
das próprias passagens. Não existe endpoint "minhas entradas/saídas". É uma
expectativa básica do usuário final.

## Objetivo
Cliente vê o histórico das próprias credenciais — onde entrou, quando, qual
aparelho.

## Critérios de aceitação
- [ ] `GET /usuario-logado/passagens` (CLIENTE e demais perfis) — lista
      paginada de `LogAcesso` cujo `Ingresso` pertence ao usuário logado.
- [ ] Filtros: `eventoId`, `de`/`ate` (ISO-8601), `resultado`.
- [ ] DTO de resposta omite dados sensíveis de outros (apenas evento/local,
      data/hora, resultado, motivo).
- [ ] `GET /ingressos/{id}/passagens` — variante por credencial específica
      (CLIENTE dono ou gestores do tenant).
- [ ] Ordenação por `dataHora DESC`, limite máximo de 100 por página.
- [ ] PERMISSIONS.md atualizado.

## Notas técnicas
- Query baseada em `Usuario.ingressos` (já mapeado).
- Filtro Hibernate `tenantFilter` cuida do isolamento.
