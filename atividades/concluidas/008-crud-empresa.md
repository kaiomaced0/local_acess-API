---
id: 008
titulo: CRUD/listagem de Empresa
prioridade: media
estimativa: M
depende-de: [001, 002]
epico: fundacao
---

## Contexto
A atividade 001 criou a entidade `Empresa` como tenant-root e expôs apenas
`POST /empresas` (restrito a `SUPER_ADMIN`). Para que o `SUPER_ADMIN` consiga
operar a instância (suspender/reativar/encerrar empresas, conferir CNPJ,
buscar uma empresa por id, listar para uso em telas administrativas), faltam
os demais endpoints. Hoje, depois de criada, uma empresa só pode ser
acessada via SQL direto.

Além disso, o login (`AuthResource` → `UsuarioService.byLoginAndSenha`) hoje
ignora o `status` da `Empresa`: um usuário de empresa `SUSPENSA`/`ENCERRADA`
continua autenticando normalmente. Isso quebra a semântica do enum
`StatusEmpresa` — se o gestor da instância suspende uma empresa por
inadimplência ou encerra por solicitação do cliente, ninguém daquele tenant
deveria conseguir entrar.

## Objetivo
- `SUPER_ADMIN` consegue listar, buscar por id, atualizar dados cadastrais
  (nome/cnpj), transicionar status (`ATIVA`/`SUSPENSA`/`ENCERRADA`) e fazer
  soft-delete de empresas.
- Login passa a rejeitar credenciais de usuários cuja empresa não está
  `ATIVA` (mesma semântica de credenciais inválidas).
- Endpoints permanecem 100% restritos a `SUPER_ADMIN` — não há gestão
  cross-tenant por parte de `ADMIN_EMPRESA`.

## Critérios de aceitação
- [ ] `GET /empresas` — lista paginada (parâmetros `page` 0-based e `size`
      com defaults `0` e `20`, `size` máx 100). Retorna apenas
      `EmpresaResponseDTO` (`id`, `nome`, `cnpj`, `status`). Soft-deleted
      (`ativo=false`) filtrados por default; opcional `incluirInativas=true`
      para auditoria.
- [ ] `GET /empresas/{id}` — busca por id. 404 quando não existir ou estiver
      soft-deleted (a menos que `?incluirInativas=true`).
- [ ] `PUT /empresas/{id}` — atualiza `nome` e `cnpj`. Não atualiza
      `status` (transição via endpoint dedicado). Conflito 409 se o CNPJ
      colidir com outra empresa.
- [ ] `PATCH /empresas/{id}/status` — transição explícita de
      `StatusEmpresa`. Body: `{ "status": "ATIVA" | "SUSPENSA" | "ENCERRADA" }`.
      Transições permitidas:
        - `ATIVA` ↔ `SUSPENSA`
        - `ATIVA` → `ENCERRADA`
        - `SUSPENSA` → `ENCERRADA`
        - `ENCERRADA` é **estado final** — qualquer tentativa de sair retorna 409.
- [ ] `DELETE /empresas/{id}` — soft-delete (`ativo=false`). Independente
      de status. Não remove fisicamente.
- [ ] Todos os endpoints novos restritos a `@RolesAllowed("SUPER_ADMIN")`.
- [ ] `byLoginAndSenha` rejeita (retorna `null`) quando `empresa.status !=
      ATIVA`, mantendo o response existente do `AuthResource` (204
      "Usuario não encontrado"). Empresas soft-deleted (`ativo=false`)
      também rejeitam login.
- [ ] Novo enum value `ENCERRADA` adicionado em `StatusEmpresa`.
- [ ] Validação de CNPJ com `@CNPJ` no DTO (quando informado).
- [ ] `PERMISSIONS.md` atualizado com as novas linhas e nota referenciando a
      atividade 008.

## Notas técnicas
- `Empresa` **não** tem `empresa_id` próprio; o filtro Hibernate
  `tenantFilter` não se aplica. Como `SUPER_ADMIN` não dispara o filtro
  (`TenantRequestFilter`), as queries em `EmpresaRepository` agem
  globalmente sem ajuste.
- O enum atual tem apenas `ATIVA`/`SUSPENSA`. A coluna no banco já é
  `VARCHAR(20)` (V2), suporta `ENCERRADA` sem migração.
- Paginação: usar `Panache.find("...").page(Page.of(p, t)).list()`.
- Soft-delete reusa `ativo=false` herdado de `EntityClass`.
- Não tocar em outras camadas — endpoints existentes de outros recursos
  permanecem inalterados.

## Resultado

### Resumo
- Adicionado novo valor `ENCERRADA` ao enum `StatusEmpresa` (estado final).
- `EmpresaResource` ganhou cinco endpoints (`GET` lista, `GET /{id}`,
  `PUT /{id}`, `PATCH /{id}/status`, `DELETE /{id}`) além do `POST` que já
  existia. Todos restritos a `@RolesAllowed("SUPER_ADMIN")` herdado da
  classe (anotação por método redundante foi removida do `POST`).
- `EmpresaService` ganhou `listar`, `buscarPorId`, `atualizar`,
  `atualizarStatus` e `softDelete`. `atualizarStatus` implementa a máquina
  de estados: `ATIVA ↔ SUSPENSA`, `ATIVA → ENCERRADA`, `SUSPENSA → ENCERRADA`;
  qualquer transição saindo de `ENCERRADA` retorna 409.
- `EmpresaRepository` ganhou `listarPaginado(incluirInativas, pageIndex,
  pageSize)` e `findByIdConsiderandoAtivo(id, incluirInativas)`.
- Novo DTO `StatusEmpresaDTO` para o body do `PATCH /{id}/status`.
- `EmpresaDTO` recebeu `@CNPJ` (validação BR via Hibernate Validator) no
  campo `cnpj` — só dispara quando o campo é informado, mantendo a
  semântica original de CNPJ opcional.
- `UsuarioService.byLoginAndSenha` agora rejeita (retorna `null`) login
  de usuários cuja empresa esteja `SUSPENSA`/`ENCERRADA` ou
  soft-deleted. Mantém o response 204 do `AuthResource` (sem expor que a
  empresa está suspensa, mesma semântica de credenciais inválidas).
- `PERMISSIONS.md`: 5 novas linhas na tabela (logo após `POST /empresas`)
  e nova nota na seção "Notas" descrevendo o escopo, as regras de
  transição e o efeito sobre o login.

### Arquivos criados
- `src/main/java/ka/mdo/dto/StatusEmpresaDTO.java`
- `atividades/em-andamento/008-crud-empresa.md` (e movido para `concluidas/`)

### Arquivos alterados
- `src/main/java/ka/mdo/model/StatusEmpresa.java`
- `src/main/java/ka/mdo/dto/EmpresaDTO.java`
- `src/main/java/ka/mdo/repository/EmpresaRepository.java`
- `src/main/java/ka/mdo/service/EmpresaService.java`
- `src/main/java/ka/mdo/resource/EmpresaResource.java`
- `src/main/java/ka/mdo/service/UsuarioService.java`
- `PERMISSIONS.md`

### Decisões não-óbvias
- **Prefixo `/api/v1` mantido** apesar da instrução da tarefa indicar que
  havia sido removido — o resto do código (todos os outros resources,
  `PERMISSIONS.md`, OpenAPI/Swagger da atividade 052) continua usando o
  prefixo. Padronização derrota inconsistência. Se a remoção for de fato
  desejada, deve ser feita em uma única atividade que toque os 5
  resources de uma vez.
- **`PATCH /{id}/status` em endpoint separado** (em vez de aceitar
  `status` no `PUT`): deixa a transição explícita, testável
  independentemente e auditável; impede que um update simples de nome
  passe `status: ENCERRADA` por engano.
- **`ENCERRADA` como absorvente**: já que o ciclo de vida econômico do
  tenant geralmente é definitivo (encerramento contratual), deixar
  reversível abriria espaço para "reativar empresa encerrada por
  engano" sem trilha de auditoria. Reativar exige nova empresa (novo
  `id`/CNPJ) — política conservadora.
- **`DELETE` é soft-delete sempre**: não há hard-delete porque excluir
  fisicamente uma empresa cascateia em milhares de linhas (usuários,
  ingressos, logs, pendências). `ativo=false` é suficiente para sumir
  da listagem default. Auditoria fica preservada via
  `?incluirInativas=true`.
- **Login rejeita silenciosamente empresa não-ATIVA**: opção deliberada
  por usar a mesma resposta 204 ("Usuario não encontrado") em vez de
  401 com mensagem "empresa suspensa". Evita expor estado interno do
  tenant a um atacante que faça enumeração.

### Build
- `./mvnw -B -ntp -DskipTests compile` → BUILD SUCCESS (133 sources).
- `./mvnw -B -ntp -DskipTests package` → falha **pré-existente** em
  `Build step io.quarkus.resteasy.reactive.common.deployment.
  ResteasyReactiveCommonProcessor#checkMixingStacks: Mixing RESTEasy
  Reactive and RESTEasy Classic server parts is not supported`. A causa
  é a coexistência de `quarkus-resteasy` (classic) com
  `quarkus-rest-client-reactive-jackson` (reactive) introduzida pela
  atividade 021. **Não foi corrigida nesta atividade** porque a tarefa
  proibiu explicitamente alterar `pom.xml`; deve ser tratada em
  atividade própria (mudar para `quarkus-rest-client-jackson` ou
  migrar todo o stack para RESTEasy Reactive).
- Verificação manual do código novo passou pelo compilador javac
  (`compile` recompila os 133 fontes sem erro nem warning relevante).

### Débitos técnicos / pendências
- **Build `package` quebrado**: ver acima. Bloqueia deploy mas não a
  evolução do código. Sugestão: criar atividade 009 ("alinha stack REST
  com cliente Frigate").
- **Sem testes**: cobertura virá com a atividade 051.
- **Trail de auditoria**: transições de status só são logadas em
  `Log.info`; idealmente registrar em uma tabela `EmpresaAuditoria`
  (quem mudou, quando, valor antigo → novo). Fica para atividade
  futura.
- **`UsuarioLogadoService`**: não foi tocado. Se um usuário já está
  logado quando a empresa é suspensa, o JWT existente continua válido
  até expirar. Mitigação: token expira em minutos; quem quiser cortar
  imediatamente precisa de blacklist de JWT (não escopo desta
  atividade).
