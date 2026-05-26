---
id: 016
titulo: Cancelamento / revogação de credencial
prioridade: media
estimativa: P
depende-de: [010, 012]
epico: credencial
---

## Contexto
Uma vez emitida, uma credencial (`Ingresso`) **não pode ser revogada** via API.
Não há `DELETE`, nem flag `revogado`, nem invalidação do token. Cenário real:
ingresso comprado e cancelado, cliente perdeu o QR, suspeita de fraude — hoje
só com acesso direto ao banco.

## Objetivo
Permitir revogar uma credencial, fazendo com que próximas leituras do token
caiam em `NEGADO CREDENCIAL_REVOGADA`.

## Critérios de aceitação
- [ ] `DELETE /ingressos/{id}` (ADMIN_EMPRESA / GESTOR_EVENTO / SUPER_ADMIN)
      marca o ingresso como revogado (campo novo `revogadoEm` + `revogadoPor`).
- [ ] `AcessoService.executarValidacao` detecta `revogadoEm != null` e retorna
      `NEGADO` com motivo `CREDENCIAL_REVOGADA` (constante nova).
- [ ] `LogAcesso` registra a tentativa com o novo motivo.
- [ ] Cliente (CLIENTE) pode revogar a **própria** credencial via
      `DELETE /ingressos/{id}` (validação de dono igual ao QR Code).
- [ ] Idempotente: revogar duas vezes responde 200 com estado atual.
- [ ] PERMISSIONS.md atualizado.

## Notas técnicas
- Migração adicionando colunas `revogadoEm` (TIMESTAMP) e `revogado_por_id`
  (FK para Usuario, NULL).
- Não deletar a linha — auditoria depende dela.
