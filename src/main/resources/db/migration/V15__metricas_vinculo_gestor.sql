-- V15: Dashboards do gestor (atividade 041).
--
-- Duas mudanças:
--
-- 1) LogAcesso.tipoMovimento (ENTRADA|SAIDA)
--    Até a 040 só registrávamos entradas. A ocupação por local dos dashboards
--    depende de poder distinguir ENTRADA de SAIDA. A coluna é NOT NULL com
--    default 'ENTRADA' para preservar semântica de leitores antigos; o default
--    é removido em seguida para que novas inserções explicitem o tipo.
--
-- 2) Tabela GestorLocal (vínculo N:N Usuario↔EspacoEvento, fecha débitos 030/031)
--    Até aqui, GESTOR_LOCAL via tudo do tenant (fila de pendências, logs e
--    autorizações). Com esta tabela, o MetricaService / PendenciaService /
--    LogAcessoService / AutorizacaoEspacoService filtram automaticamente pela
--    lista de locais vinculados quando o chamador é GESTOR_LOCAL puro.

-- ---- 1) LogAcesso.tipoMovimento ------------------------------------------
ALTER TABLE LogAcesso
    ADD COLUMN tipoMovimento VARCHAR(20) NOT NULL DEFAULT 'ENTRADA';

-- Remove o default: novas inserções (via Hibernate) devem sempre setar o
-- campo — o default só existe para a migração preencher linhas existentes
-- sem travar (backfill implícito). MariaDB e Postgres aceitam este ALTER.
ALTER TABLE LogAcesso
    ALTER COLUMN tipoMovimento DROP DEFAULT;

-- ---- 2) GestorLocal ------------------------------------------------------
CREATE TABLE GestorLocal (
    id               BIGINT    NOT NULL AUTO_INCREMENT,
    dataInclusao     TIMESTAMP NULL,
    ativo            BOOLEAN   NOT NULL DEFAULT TRUE,
    usuario_id       BIGINT    NOT NULL,
    espaco_evento_id BIGINT    NOT NULL,
    empresa_id       BIGINT    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_gestorlocal_usuario
        FOREIGN KEY (usuario_id)       REFERENCES Usuario (id),
    CONSTRAINT fk_gestorlocal_espacoevento
        FOREIGN KEY (espaco_evento_id) REFERENCES EspacoEvento (id),
    CONSTRAINT fk_gestorlocal_empresa
        FOREIGN KEY (empresa_id)       REFERENCES Empresa (id),
    CONSTRAINT uk_gestorlocal_usuario_local
        UNIQUE (usuario_id, espaco_evento_id)
);

-- Índices de leitura: listar locais de um usuário (MetricaService,
-- PendenciaService, LogAcessoService) e listar gestores de um local
-- (PendenciaService.notificarGestores).
CREATE INDEX idx_gestorlocal_usuario  ON GestorLocal (usuario_id);
CREATE INDEX idx_gestorlocal_local    ON GestorLocal (espaco_evento_id);
CREATE INDEX idx_gestorlocal_empresa  ON GestorLocal (empresa_id);
