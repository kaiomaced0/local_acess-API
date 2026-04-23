-- V6: cria tabela Aparelho (atividade 011).
--
-- Um Aparelho é um dispositivo físico (totem/leitor/tablet) que opera sob
-- um perfil OPERADOR_APARELHO e consome POST /api/v1/acesso/validar para
-- decidir liberar ou bloquear acessos.
--
-- Multitenancy: empresa_id NOT NULL, mesmo padrão dos demais negócios.
--   - local_especifico_id NULL => aparelho opera na entrada do evento.
--   - local_especifico_id != NULL => aparelho restringe acesso àquele
--     EspacoEvento específico.
--   - evento_id NULL => aparelho genérico (pode operar em qualquer evento
--     ativo da empresa). Não-null vincula o aparelho a um único evento.
--
-- ResultadoAcesso é enum Java persistido apenas em memória por enquanto
-- (atividade 012 criará LogAcesso e vai usar enum como coluna STRING).
-- Por isso não há tabela para ele aqui.

CREATE TABLE Aparelho (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    dataInclusao         TIMESTAMP     NULL,
    ativo                BOOLEAN       NOT NULL DEFAULT TRUE,
    descricao            VARCHAR(255)  NULL,
    empresa_id           BIGINT        NOT NULL,
    local_especifico_id  BIGINT        NULL,
    evento_id            BIGINT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_aparelho_empresa
        FOREIGN KEY (empresa_id) REFERENCES Empresa (id),
    CONSTRAINT fk_aparelho_espacoevento
        FOREIGN KEY (local_especifico_id) REFERENCES EspacoEvento (id),
    CONSTRAINT fk_aparelho_evento
        FOREIGN KEY (evento_id) REFERENCES Evento (id)
);

-- Índice por empresa para suportar o filtro de tenant e listagens futuras.
CREATE INDEX ix_aparelho_empresa ON Aparelho (empresa_id);
