-- V3: adiciona empresa_id NOT NULL + FK em todas as tabelas de negócio.
--
-- Estratégia portável (MariaDB + PostgreSQL): criar a coluna já como NOT NULL
-- DEFAULT 1, apontando para a empresa padrão inserida abaixo, e então remover o
-- default. Em seguida cria-se a FK. Essa forma evita a divergência de sintaxe
-- entre `ALTER ... MODIFY` (MariaDB) e `ALTER ... ALTER COLUMN ... SET NOT NULL`
-- (PostgreSQL).

-- Empresa padrão para backfill de linhas pré-existentes.
INSERT INTO Empresa (id, dataInclusao, ativo, nome, cnpj, status)
VALUES (1, CURRENT_TIMESTAMP, TRUE, 'Empresa Padrão', '00000000000000', 'ATIVA');

-- ============================================================================
-- Evento
-- ============================================================================
ALTER TABLE Evento ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE Evento ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE Evento ADD CONSTRAINT fk_evento_empresa
    FOREIGN KEY (empresa_id) REFERENCES Empresa (id);

-- ============================================================================
-- EspacoEvento
-- ============================================================================
ALTER TABLE EspacoEvento ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE EspacoEvento ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE EspacoEvento ADD CONSTRAINT fk_espacoevento_empresa
    FOREIGN KEY (empresa_id) REFERENCES Empresa (id);

-- ============================================================================
-- TipoIngresso
-- ============================================================================
ALTER TABLE TipoIngresso ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE TipoIngresso ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE TipoIngresso ADD CONSTRAINT fk_tipoingresso_empresa
    FOREIGN KEY (empresa_id) REFERENCES Empresa (id);

-- ============================================================================
-- Ingresso
-- ============================================================================
ALTER TABLE Ingresso ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE Ingresso ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE Ingresso ADD CONSTRAINT fk_ingresso_empresa
    FOREIGN KEY (empresa_id) REFERENCES Empresa (id);

-- ============================================================================
-- Usuario
-- ============================================================================
ALTER TABLE Usuario ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE Usuario ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE Usuario ADD CONSTRAINT fk_usuario_empresa
    FOREIGN KEY (empresa_id) REFERENCES Empresa (id);
