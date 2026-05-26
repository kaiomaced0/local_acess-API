---
id: 055
titulo: Rate limiting em endpoints sensíveis
prioridade: baixa
estimativa: P
depende-de: [002]
epico: observabilidade
---

## Contexto
Não há limitação de taxa em nenhum endpoint. `POST /auth` aceita brute-force
ilimitado, `POST /acesso/validar` aceita flood de qualquer operador
comprometido, `POST /auth/esqueci-senha` (atividade 005) seria vetor de
enumeração + spam de email.

## Objetivo
Throttle por IP e por subject (JWT sub) nos endpoints de maior risco.

## Critérios de aceitação
- [ ] Filtro JAX-RS que conta requisições por janela deslizante
      (token bucket simples em memória ou Redis se houver).
- [ ] Anotação `@RateLimit(porIp=N, porSubject=M, janela="1m")` em endpoints
      sensíveis: `/auth`, `/auth/esqueci-senha`, `/auth/redefinir-senha`,
      `/acesso/validar`, `/acesso/validar-com-foto`.
- [ ] Resposta 429 Too Many Requests com header `Retry-After`.
- [ ] Configuração por endpoint via `application.properties`.
- [ ] SUPER_ADMIN tem limites mais permissivos (configurável).
- [ ] Métrica exportada via SmallRye Metrics: contagem de 429 por endpoint.

## Notas técnicas
- Em memória basta para uma instância. Para HA, integrar com Redis depois.
- Considerar extensão `quarkus-smallrye-fault-tolerance` (tem `@RateLimit` em
  versões recentes) ou rolar uma implementação enxuta.
