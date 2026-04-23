---
id: 051
titulo: Suite mínima de testes de integração
prioridade: media
estimativa: M
depende-de: [001, 011]
epico: observabilidade
---

## Contexto
Sem testes de integração, mudanças estruturais (multitenancy, pendências, Frigate) são arriscadas.

## Objetivo
Cobertura mínima dos fluxos críticos com `@QuarkusTest` + rest-assured + Testcontainers.

## Critérios de aceitação
- [ ] Testcontainers para Postgres/MariaDB no profile de teste.
- [ ] Testes: login/emissão de JWT, isolamento entre empresas, emissão de credencial, validação de acesso (3 resultados), aprovação de pendência.
- [ ] Rodam via `./mvnw test` sem serviços externos.
- [ ] Pipeline CI (arquivo `.github/workflows/ci.yml` ou equivalente) executando os testes em cada push.

## Notas técnicas
- Frigate e storage mockados por `@QuarkusMock` ou wiremock.
