---
id: 019
titulo: Importação em lote de credenciais (CSV)
prioridade: baixa
estimativa: M
depende-de: [010, 013, 015]
epico: credencial
---

## Contexto
Eventos com vendas online ou listas pré-cadastradas precisam emitir
centenas/milhares de credenciais. Fazer N chamadas a `POST /usuarios/{id}/ingressos`
é lento e propenso a falhas parciais sem rastreabilidade.

## Objetivo
Upload de CSV cria usuários (se necessário) + credenciais em lote, com
relatório de sucesso/erro por linha.

## Critérios de aceitação
- [ ] `POST /ingressos/importar` (ADMIN_EMPRESA / GESTOR_EVENTO) aceita CSV
      (`text/csv` ou multipart) com colunas:
      `nome,email,cpf,telefone,tipoIngressoNome,chaveAcesso,lote`.
- [ ] Cria/atualiza `Usuario` (perfil `CLIENTE`) e emite `Ingresso` por linha.
- [ ] Resposta: `{ totalLinhas, sucesso, falhas: [{ linha, erro }] }`.
- [ ] Toda a operação em uma transação por linha (linhas independentes; uma
      ruim não derruba o lote).
- [ ] Tamanho máximo: 10.000 linhas por requisição (configurável).
- [ ] Validação de duplicidade: `(cpf, evento)` já com credencial ⇒ falha
      controlada na linha.
- [ ] Log estruturado por importação (request-id agrupa as inserções).

## Notas técnicas
- Bibliotecas: Apache Commons CSV ou Jackson CSV.
- Considerar fila assíncrona para lotes muito grandes (out of scope nesta
  iteração; sync já cobre os casos típicos).
