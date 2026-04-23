-- V10: integração com Frigate / validação facial (atividade 021).
--
-- Evento ganha flag `validarFacial` (default FALSE, preserva comportamento
-- de eventos existentes).
--
-- DadosPessoais ganha:
--   * rostoFrigateCadastrado — true após o enrolamento do rosto no Frigate
--     na primeira leitura de um evento com validarFacial=true. Leituras
--     seguintes só comparam.
--   * frigatePessoaId — identificador do rosto no Frigate (string). Separado
--     de Usuario.id porque o Frigate trabalha com strings e queremos
--     desacoplar do nosso esquema interno.
--
-- Padrão portável (MariaDB + PostgreSQL): mesmo estilo da V3/V8 (DEFAULT
-- explícito, sem sintaxe de modify).

ALTER TABLE Evento
    ADD COLUMN validarFacial BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE DadosPessoais
    ADD COLUMN rostoFrigateCadastrado BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE DadosPessoais
    ADD COLUMN frigatePessoaId VARCHAR(100);
