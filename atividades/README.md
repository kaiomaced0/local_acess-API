# Gestão de atividades — local_acess-API

Backlog versionado em arquivos markdown. Cada atividade vive em uma das três pastas:

- `backlog/` — não iniciada
- `em-andamento/` — em execução (mover o arquivo ao começar)
- `concluidas/` — finalizada (adicionar seção `## Resultado` com resumo + arquivos alterados)

## Nome do arquivo

`NNN-slug-curto.md`, onde `NNN` agrupa por épico:

| Faixa | Épico |
|-------|-------|
| 001–009 | Fundação (multitenancy, perfis, migrações) |
| 010–019 | Credenciais e QR Code |
| 020–029 | Dados pessoais e validação facial (Frigate) |
| 030–039 | Locais, perfis de acesso, pendências, notificações |
| 040–049 | Mapa 2D e dashboards de gestão |
| 050–059 | Observabilidade e qualidade |

## Formato de uma atividade

```markdown
---
id: 001
titulo: Multitenancy — entidade Empresa
prioridade: alta        # alta | media | baixa
estimativa: M           # P | M | G
depende-de: []          # lista de ids
epico: fundacao
---

## Contexto
Por que essa atividade existe. Que problema ela resolve.

## Objetivo
O que vai existir no sistema depois que ela for entregue.

## Critérios de aceitação
- [ ] Item verificável 1
- [ ] Item verificável 2

## Notas técnicas
Dicas, arquivos envolvidos, dependências candidatas, riscos.
```

## Ao concluir

Mover para `concluidas/` e anexar:

```markdown
## Resultado
- Arquivos alterados: `src/...`
- Commits: `abc123`, `def456`
- Observações pós-entrega (o que ficou para depois)
```
