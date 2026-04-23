-- V9: serviço de notificações (atividade 032).
--
-- Cria a entidade canônica Notificacao e a tabela de preferências de canal
-- do usuário. A gravação da notificação é síncrona (na transação de quem
-- chama NotificacaoService.enviar). A entrega por canal roda em listeners
-- assíncronos (CDI ObservesAsync) — cada listener em sua própria transação
-- (REQUIRES_NEW) para não bloquear o happy path.
--
-- Relacionamentos:
--   - empresa_id     NOT NULL  => multitenancy (filtro Hibernate tenantFilter).
--   - destinatario_id NOT NULL => sempre um único usuário por notificação.
--
-- Decisões:
--   - payloadJson em TEXT: JSON arbitrário por tipo (ex.: id da pendência
--     quando tipo = PENDENCIA_ABERTA). Indexação textual desnecessária.
--   - lida: boolean simples; a UI consulta "não lidas" por
--     (destinatario, lida=false). Índice cobre esse caminho.
--   - criadaEm: campo separado de dataInclusao (herdado de EntityClass)
--     para garantir semântica explícita — o que o usuário lê é criadaEm.

CREATE TABLE Notificacao (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    dataInclusao   TIMESTAMP     NULL,
    ativo          BOOLEAN       NOT NULL DEFAULT TRUE,
    empresa_id     BIGINT        NOT NULL,
    destinatario_id BIGINT       NOT NULL,
    tipo           VARCHAR(40)   NOT NULL,
    titulo         VARCHAR(150)  NOT NULL,
    mensagem       VARCHAR(500)  NOT NULL,
    payloadJson    TEXT          NULL,
    lida           BOOLEAN       NOT NULL DEFAULT FALSE,
    criadaEm       TIMESTAMP     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notificacao_empresa
        FOREIGN KEY (empresa_id) REFERENCES Empresa (id),
    CONSTRAINT fk_notificacao_destinatario
        FOREIGN KEY (destinatario_id) REFERENCES Usuario (id)
);

-- Índice para o caminho crítico do painel: "últimas N não-lidas do usuário".
-- Ordem DESC em criadaEm é sugestão — engines comuns (MariaDB/Postgres)
-- sabem percorrer ASC também; mantemos a direção para documentar a intenção.
CREATE INDEX idx_notificacao_destinatario_lida_data
    ON Notificacao (destinatario_id, lida, criadaEm);

-- Tabela de associação para @ElementCollection Set<CanalNotificacao>.
-- Uma linha por canal habilitado por usuário. Sem PK composta explícita —
-- Hibernate não exige para ElementCollection sem @OrderColumn.
CREATE TABLE usuario_canais_notificacao (
    usuario_id BIGINT      NOT NULL,
    canal      VARCHAR(20) NOT NULL,
    CONSTRAINT fk_uncan_usuario
        FOREIGN KEY (usuario_id) REFERENCES Usuario (id)
);

CREATE INDEX idx_uncan_usuario ON usuario_canais_notificacao (usuario_id);

-- Seed: cada usuário existente recebe os canais default (WEBSOCKET e EMAIL).
-- PUSH fica de fora enquanto FCM é stub.
INSERT INTO usuario_canais_notificacao (usuario_id, canal)
SELECT id, 'WEBSOCKET' FROM Usuario;

INSERT INTO usuario_canais_notificacao (usuario_id, canal)
SELECT id, 'EMAIL' FROM Usuario;
