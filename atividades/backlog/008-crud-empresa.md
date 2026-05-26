---
id: 008
titulo: CRUD e listagem de Empresa
prioridade: alta
estimativa: P
depende-de: [001]
epico: fundacao
---

## Contexto
`EmpresaResource` só tem `POST /empresas` (SUPER_ADMIN). Não há `GET`, `GET /{id}`,
`PUT`, status (ATIVA/SUSPENSA/etc.). O painel do SUPER_ADMIN não consegue listar
empresas existentes nem suspendê-las.

## Objetivo
Cobertura completa do recurso `Empresa` para administração da instância.

## Critérios de aceitação
- [ ] `GET /empresas` (SUPER_ADMIN) — lista paginada, filtros opcionais por
      `status` e `nome` (LIKE).
- [ ] `GET /empresas/{id}` (SUPER_ADMIN).
- [ ] `PUT /empresas/{id}` (SUPER_ADMIN) — edita nome, cnpj (com validação de
      duplicidade), status (`StatusEmpresa`).
- [ ] `PATCH /empresas/{id}/suspender` e `/reativar` — atalhos sobre `status`.
- [ ] `DELETE /empresas/{id}` — soft-delete (`ativo=false`); não deleta dados.
- [ ] Empresa com `status != ATIVA` rejeita logins de usuários do tenant
      (`UsuarioService.byLoginAndSenha` valida).
- [ ] Atualizar PERMISSIONS.md.

## Notas técnicas
- Filtro Hibernate `tenantFilter` **não** se aplica a `Empresa` (não tem
  `empresa_id` próprio).
- Validação de CNPJ via `@CNPJ` (hibernate-validator-br).
