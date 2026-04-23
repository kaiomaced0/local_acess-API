-- V11: whitelist de TipoIngresso por EspacoEvento (atividade 030).
--
-- Tabela de associação N:N:
--   - PK composta (espaco_evento_id, tipo_ingresso_id) garante unicidade sem
--     precisar de coluna de id artificial.
--   - FKs restritas para evitar órfãos; ON DELETE CASCADE não é aplicado aqui
--     porque o soft-delete do app é a regra (flag `ativo`).
--
-- Padrão portável MariaDB + PostgreSQL (alinhado com V6/V8/V9/V10).

CREATE TABLE espacoevento_tipo_ingresso_autorizado (
    espaco_evento_id   BIGINT NOT NULL,
    tipo_ingresso_id   BIGINT NOT NULL,
    PRIMARY KEY (espaco_evento_id, tipo_ingresso_id),
    CONSTRAINT fk_eetia_espacoevento
        FOREIGN KEY (espaco_evento_id) REFERENCES EspacoEvento (id),
    CONSTRAINT fk_eetia_tipoingresso
        FOREIGN KEY (tipo_ingresso_id) REFERENCES TipoIngresso (id)
);

-- Índice inverso para acelerar consultas "em quais locais este tipo é
-- autorizado?" (reportaria a atividade 041 / dashboards).
CREATE INDEX ix_eetia_tipo ON espacoevento_tipo_ingresso_autorizado (tipo_ingresso_id);

-- Log de auditoria de mudanças de autorização por local.
--
-- acao: string (enum Java AcaoAutorizacao = ADICIONADO | REMOVIDO | SUBSTITUIDO).
--   Persistido como VARCHAR para manter legibilidade em consultas ad-hoc
--   (mesmo padrão dos demais enums do projeto).
-- tipo_ingresso_id: NULL para SUBSTITUIDO (onde a lista inteira foi trocada);
--   NOT NULL para ADICIONADO/REMOVIDO.
-- usuario_id: referência "soft" (sem FK). Evita acoplar auditoria ao ciclo
--   de vida do Usuario (soft-delete não deve apagar logs).
CREATE TABLE AutorizacaoAuditoria (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    dataInclusao       TIMESTAMP     NULL,
    ativo              BOOLEAN       NOT NULL DEFAULT TRUE,
    empresa_id         BIGINT        NOT NULL,
    espaco_evento_id   BIGINT        NOT NULL,
    acao               VARCHAR(20)   NOT NULL,
    tipo_ingresso_id   BIGINT        NULL,
    usuario_id         BIGINT        NOT NULL,
    dataHora           TIMESTAMP     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_autaud_empresa
        FOREIGN KEY (empresa_id) REFERENCES Empresa (id),
    CONSTRAINT fk_autaud_espacoevento
        FOREIGN KEY (espaco_evento_id) REFERENCES EspacoEvento (id)
);

-- Índices para consultas de auditoria (tempo + tenant + espaço).
CREATE INDEX ix_autaud_empresa_data ON AutorizacaoAuditoria (empresa_id, dataHora);
CREATE INDEX ix_autaud_espaco ON AutorizacaoAuditoria (espaco_evento_id, dataHora);
