-- V7: cria tabela LogAcesso (atividade 012).
--
-- Registra cada decisão de validação tomada pelos aparelhos (AUTORIZADO /
-- PENDENTE / NEGADO) para fins de auditoria, dashboards e resolução de
-- disputas. Gravação é assíncrona (CDI ObservesAsync em LogAcessoService),
-- então a latência do endpoint POST /acesso/validar não é afetada
-- por esta tabela.
--
-- Relacionamentos:
--   - empresa_id NOT NULL => multitenancy (filtro Hibernate tenantFilter).
--   - ingresso_id NULL    => permitimos logar tentativas com token
--                            inexistente (credencial não encontrada).
--   - local_id   NULL     => aparelhos de entrada geral do evento podem
--                            operar sem EspacoEvento específico.
--   - aparelho_id NOT NULL => toda decisão precisa vir de um aparelho
--                            real (caso aparelho sumir entre a decisão e o
--                            log, o listener apenas registra log.error e
--                            abandona — nunca insere placeholder).
--
-- Resultado: enum ResultadoAcesso persistido como STRING (20 chars).
-- Motivo: código curto estável — mesmo devolvido ao aparelho no DTO.
-- fotoCapturadaUrl: placeholder para a atividade 021 (validação facial).
--
-- TODO futuro: particionar por mês se volume de leituras exigir.

CREATE TABLE LogAcesso (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    dataInclusao      TIMESTAMP     NULL,
    ativo             BOOLEAN       NOT NULL DEFAULT TRUE,
    empresa_id        BIGINT        NOT NULL,
    ingresso_id       BIGINT        NULL,
    local_id          BIGINT        NULL,
    aparelho_id       BIGINT        NOT NULL,
    resultado         VARCHAR(20)   NOT NULL,
    motivo            VARCHAR(100)  NULL,
    dataHora          TIMESTAMP     NOT NULL,
    fotoCapturadaUrl  VARCHAR(500)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_logacesso_empresa
        FOREIGN KEY (empresa_id)  REFERENCES Empresa (id),
    CONSTRAINT fk_logacesso_ingresso
        FOREIGN KEY (ingresso_id) REFERENCES Ingresso (id),
    CONSTRAINT fk_logacesso_local
        FOREIGN KEY (local_id)    REFERENCES EspacoEvento (id),
    CONSTRAINT fk_logacesso_aparelho
        FOREIGN KEY (aparelho_id) REFERENCES Aparelho (id)
);

-- Índices recomendados na atividade 012:
--   (empresa_id, dataHora) — dashboards por tenant ordenados por tempo.
--   (ingresso_id, dataHora) — histórico por credencial (resolução de
--                             disputa: "onde esse ingresso passou?").
CREATE INDEX idx_log_acesso_empresa_data   ON LogAcesso (empresa_id, dataHora);
CREATE INDEX idx_log_acesso_credencial_data ON LogAcesso (ingresso_id, dataHora);
