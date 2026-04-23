---
name: desenvolvedor
description: Skill para implementar features e corrigir bugs na API Quarkus do local_acess seguindo os padrões do projeto. Use quando a tarefa envolver escrever código Java, criar entidades JPA, endpoints REST, services, DTOs ou migrações.
---

# Skill Desenvolvedor — local_acess-API

Você é o desenvolvedor responsável pela API do sistema de controle de acesso a eventos e locais.

## Stack
- Java 17
- Quarkus 3.5.3
- Hibernate ORM com Panache
- REST (resteasy + jackson)
- JWT (smallrye-jwt)
- MariaDB / PostgreSQL
- Hibernate Validator

## Estrutura de pacotes (ka.mdo)
- `model` — entidades JPA (extendem `EntityClass`)
- `dto` — request/response DTOs
- `converter` — conversões entity ↔ DTO
- `repository` — `PanacheRepository<T>`
- `service` — regras de negócio, `@ApplicationScoped`
- `resource` — endpoints REST (`@Path`)

## Padrões obrigatórios

1. **Entidades**: sempre extender `EntityClass`, usar `@Entity` e quando houver herança `@Inheritance(strategy = InheritanceType.JOINED)`.
2. **Multitenancy**: toda entidade de negócio precisa ter referência a `Empresa` (tenant). Consultas devem filtrar por `empresaId` do JWT.
3. **DTOs**: nunca expor entidades diretamente. Criar `XxxDTO` (request) e `XxxResponseDTO` (response).
4. **Validação**: usar `jakarta.validation` nos DTOs (`@NotNull`, `@NotBlank`, `@Email`, `@Size`).
5. **Senhas**: sempre hash via `HashService` — nunca persistir em claro.
6. **JWT**: endpoints protegidos com `@RolesAllowed`. Perfis previstos: `ADMIN_EMPRESA`, `GESTOR_EVENTO`, `GESTOR_LOCAL`, `OPERADOR_APARELHO`, `CLIENTE`.
7. **Commits**: mensagens em português, imperativo curto (ex: "adiciona QR code na credencial").

## Ao implementar uma atividade
1. Leia a atividade em `atividades/backlog/` ou `atividades/em-andamento/`.
2. Mova o arquivo para `atividades/em-andamento/` ao iniciar.
3. Implemente seguindo os padrões acima.
4. Escreva testes quando aplicável (`quarkus-junit5` + `rest-assured`).
5. Ao concluir, mova o arquivo para `atividades/concluidas/` e adicione seção `## Resultado` com resumo e arquivos alterados.

## Ao introduzir nova dependência
Justifique no PR/commit e atualize `pom.xml`. Dependências candidatas conhecidas:
- `zxing` (QR Code)
- `quarkus-minio` ou `quarkus-amazon-s3` (armazenamento de imagens)
- `quarkus-websockets-next` (notificações tempo real)
- `quarkus-messaging-kafka` ou `quarkus-mailer` (notificações)

## Regras de domínio-chave
- **Credencial** = `Ingresso` com `token` único → gera QR Code.
- **Validação facial**: se o evento exige, a primeira leitura precisa cadastrar/validar rosto via Frigate; leituras posteriores comparam. Se falhar → status `PENDENTE`.
- **Pendências**: gestor de local/evento recebe notificação. Ação dele (aprovar/recusar) notifica o dono da credencial.
- **Usuário global**: flag que permite acesso a todos os eventos/locais da empresa (ou cross-tenant quando `superGlobal`).
- **Mapa 2D do evento**: polígonos/retângulos com cor representando cada `EspacoEvento`.
