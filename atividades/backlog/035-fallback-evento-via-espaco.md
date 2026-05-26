---
id: 035
titulo: Fallback de Evento via FK em EspacoEvento (TODO 030)
prioridade: media
estimativa: M
depende-de: [030]
epico: locais
---

## Contexto
`AcessoService.resolverEventoContexto` hoje pega o evento de `aparelho.getEvento()`
e, se for null, retorna null — pula validação de período do evento. Há
`// TODO 030` em `AcessoService.java:481` indicando o caminho desejado:
quando o aparelho é genérico (sem evento), inferir o evento via
`EspacoEvento.evento` quando `localId` está presente. Hoje `EspacoEvento` nem
tem FK para `Evento` (a relação é Evento→EspacoEvento via @OneToMany).

## Objetivo
Permitir que aparelhos genéricos posicionados em um local específico ainda
validem o período do evento ao qual o local pertence.

## Critérios de aceitação
- [ ] Migração que adiciona `evento_id` em `EspacoEvento` (NULL inicial,
      backfill via `SELECT id FROM Evento WHERE Evento.id IN
      (espaco do owner)`), depois NOT NULL.
- [ ] Entidade `EspacoEvento` ganha `@ManyToOne Evento evento`.
- [ ] `AcessoService.resolverEventoContexto`: se `aparelho.evento == null`
      e `localId != null`, retorna `espacoEventoRepository.findById(localId)
      .getEvento()`.
- [ ] TODO 030 removido do código.
- [ ] Teste de integração: aparelho genérico + local em evento expirado ⇒
      `NEGADO EVENTO_FORA_DO_PERIODO`.

## Notas técnicas
- Conferir consistência: `Evento.espacoEventos` (lista) hoje é a única ponta
  da relação. A nova FK em `EspacoEvento` é o lado dono — manter ambos
  sincronizados ou tornar o `@OneToMany` em `mappedBy`.
