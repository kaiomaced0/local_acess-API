-- V8: dados pessoais do dono da credencial (atividade 020).
--
-- Motivação: eventos podem exigir identificação completa do portador
-- (nome, documento, foto). O AcessoService consulta esses dados quando
-- Evento.exigeDadosPessoais = TRUE e retorna PENDENTE com motivo
-- DADOS_PESSOAIS_INCOMPLETOS caso estejam faltando.
--
-- Decisão de modelo: vinculamos DadosPessoais ao Usuario (1:1), não ao
-- Ingresso. Um usuário pode ter várias credenciais — compartilhar os
-- mesmos dados evita duplicação (LGPD: menos cópias do documento em
-- banco). A FK fica no lado Usuario (dados_pessoais_id) para manter o
-- JOIN natural a partir do dono da credencial no AcessoService.
--
-- LGPD / criptografia em repouso: o documento é persistido em claro na
-- coluna. A garantia de criptografia em repouso é dada pela camada de
-- infra (banco com TDE em produção + bucket com SSE para as fotos) —
-- ver seção "Criptografia em repouso" no resultado da atividade 020.
-- Não introduzimos criptografia aplicacional (custo de gerência de
-- chaves não justifica no estágio atual).
--
-- Fotos: persistimos apenas a chave do objeto nos buckets
-- `credenciais-foto` (selfie) e `documentos` (foto do doc). URLs
-- pré-assinadas são geradas a cada leitura via StorageService.

CREATE TABLE DadosPessoais (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    dataInclusao              TIMESTAMP     NULL,
    ativo                     BOOLEAN       NOT NULL DEFAULT TRUE,
    empresa_id                BIGINT        NOT NULL,
    nomeCompleto              VARCHAR(150)  NOT NULL,
    tipoDocumento             VARCHAR(20)   NOT NULL,
    documento                 VARCHAR(40)   NOT NULL,
    dataNascimento            DATE          NULL,
    fotoObjectKey             VARCHAR(255)  NULL,
    documentoFotoObjectKey    VARCHAR(255)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_dadospessoais_empresa
        FOREIGN KEY (empresa_id) REFERENCES Empresa (id),
    CONSTRAINT uk_dadospessoais_empresa_tipo_doc
        UNIQUE (empresa_id, tipoDocumento, documento)
);

-- Índice para busca por documento dentro do tenant (repository.findByDocumento).
-- A UNIQUE já serve como índice composto, mas mantemos o índice explícito
-- só com (empresa_id, documento) para buscas que não filtram por tipo.
CREATE INDEX ix_dadospessoais_empresa_documento
    ON DadosPessoais (empresa_id, documento);

-- FK do lado Usuario para DadosPessoais (1:1). NULL permitido: usuários
-- legados e clientes que ainda não preencheram continuam válidos.
ALTER TABLE Usuario
    ADD COLUMN dados_pessoais_id BIGINT NULL;

ALTER TABLE Usuario
    ADD CONSTRAINT fk_usuario_dadospessoais
        FOREIGN KEY (dados_pessoais_id) REFERENCES DadosPessoais (id);

-- Flag no Evento: default FALSE preserva comportamento para eventos
-- existentes (não exigem dados pessoais até que um gestor ative).
ALTER TABLE Evento
    ADD COLUMN exigeDadosPessoais BOOLEAN NOT NULL DEFAULT FALSE;
