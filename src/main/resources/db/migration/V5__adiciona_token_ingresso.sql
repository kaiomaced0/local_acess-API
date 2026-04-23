-- V5: adiciona coluna `token` em Ingresso + índice único.
--
-- Contexto: atividade 010 — cada credencial passa a ter um token opaco,
-- cripto-seguro, único por linha. Esse token é o payload do QR Code e do
-- endpoint de validação de acesso (atividade 011).
--
-- Estratégia portável (MariaDB + PostgreSQL):
--
-- 1. Criar a coluna já como NOT NULL DEFAULT '' para aceitar o INSERT de
--    linhas legadas sem precisar de função UUID específica (MariaDB usa
--    UUID(); PostgreSQL usa gen_random_uuid() — não temos um dialeto em
--    comum). Em seguida removemos o DEFAULT, igual ao padrão usado na V3
--    para empresa_id.
--
-- 2. Popular linhas pré-existentes com um marcador determinístico e único
--    ('legacy-<id>'). Isso garante unicidade mesmo em bancos com registros
--    herdados, e permite que o índice UNIQUE seja criado imediatamente.
--    Após a subida da aplicação, um bootstrap (ka.mdo.service.BackfillTokenService)
--    substitui esses tokens 'legacy-*' por valores cripto-seguros reais
--    vindos de TokenService — sem downtime, idempotente.
--
-- 3. Índice único `uk_ingresso_token` criado após o backfill inicial.
--    Novas credenciais criadas via IngressoService sempre recebem token
--    cripto-seguro de 256 bits na persistência, então o UNIQUE é preservado.
--
-- Não há risco de colisão entre os placeholders 'legacy-<id>' e os tokens
-- reais (base64url de 43 chars sem hífen como primeiro token).

-- Passo 1: adicionar coluna com default vazio para aceitar registros legados.
ALTER TABLE Ingresso ADD COLUMN token VARCHAR(64) NOT NULL DEFAULT '';
ALTER TABLE Ingresso ALTER COLUMN token DROP DEFAULT;

-- Passo 2: backfill determinístico e único para qualquer linha que já exista.
-- Linhas inseridas após esta migração pelo IngressoService sempre terão um
-- token cripto-seguro — o UPDATE abaixo só afeta registros pré-V5.
UPDATE Ingresso
   SET token = CONCAT('legacy-', CAST(id AS CHAR(20)))
 WHERE token = '' OR token IS NULL;

-- Passo 3: índice único. A aplicação substituirá os 'legacy-*' restantes
-- por tokens cripto-seguros reais no startup (BackfillTokenService), sem
-- quebrar a constraint porque a substituição é 1-para-1 (id -> novo token).
CREATE UNIQUE INDEX uk_ingresso_token ON Ingresso(token);
