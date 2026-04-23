-- V14: Mapa 2D do evento (atividade 040).
--
-- MapaEvento é 1:1 com Evento (UNIQUE em evento_id) e agrega N FormaMapa.
-- Cada FormaMapa referencia um EspacoEvento e guarda a geometria como JSON
-- serializado em TEXT — o parse/validação ocorre em MapaEventoService, não
-- precisamos de PostGIS nem JSON funcional do banco para este caso de uso.
--
-- Decisões:
--   - empresa_id NOT NULL em MapaEvento: multitenancy (tenantFilter).
--   - evento_id UNIQUE: garante no schema o invariante "um mapa por evento"
--     — evita duplicatas se houver concorrência no PUT.
--   - imagemFundoObjectKey VARCHAR(500) NULL: opcional; chave no bucket
--     `mapas` (ver storage.bucket.mapas e StorageBucketBootstrap).
--   - unidade VARCHAR(20): metadado livre ("px", "m", "ft") apenas para
--     rotulagem no frontend.
--   - FormaMapa.geometriaJson TEXT NOT NULL: formato varia por tipo
--     (ver TipoForma). TEXT em vez de JSON para compatibilidade entre
--     MariaDB e PostgreSQL sem conversor específico.
--   - corHex VARCHAR(7) NOT NULL: formato HTML curto (#RRGGBB), validado
--     em application via regex.
--   - rotulo VARCHAR(100) NULL: legenda opcional na forma.

CREATE TABLE MapaEvento (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    dataInclusao         TIMESTAMP    NULL,
    ativo                BOOLEAN      NOT NULL DEFAULT TRUE,
    empresa_id           BIGINT       NOT NULL,
    evento_id            BIGINT       NOT NULL,
    largura              INT          NOT NULL DEFAULT 1000,
    altura               INT          NOT NULL DEFAULT 1000,
    unidade              VARCHAR(20)  NOT NULL DEFAULT 'px',
    imagemFundoObjectKey VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_mapaevento_evento UNIQUE (evento_id),
    CONSTRAINT fk_mapaevento_empresa
        FOREIGN KEY (empresa_id) REFERENCES Empresa (id),
    CONSTRAINT fk_mapaevento_evento
        FOREIGN KEY (evento_id)  REFERENCES Evento (id)
);

CREATE INDEX idx_mapaevento_empresa ON MapaEvento (empresa_id);

CREATE TABLE FormaMapa (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    dataInclusao  TIMESTAMP    NULL,
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    mapa_id       BIGINT       NOT NULL,
    espaco_id     BIGINT       NOT NULL,
    tipo          VARCHAR(20)  NOT NULL,
    geometriaJson TEXT         NOT NULL,
    corHex        VARCHAR(7)   NOT NULL,
    rotulo        VARCHAR(100) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_formamapa_mapa
        FOREIGN KEY (mapa_id)   REFERENCES MapaEvento (id),
    CONSTRAINT fk_formamapa_espaco
        FOREIGN KEY (espaco_id) REFERENCES EspacoEvento (id)
);

CREATE INDEX idx_formamapa_mapa   ON FormaMapa (mapa_id);
CREATE INDEX idx_formamapa_espaco ON FormaMapa (espaco_id);
