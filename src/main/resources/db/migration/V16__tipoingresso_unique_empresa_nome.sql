-- V16: Unicidade do nome de TipoIngresso por empresa (atividade 015).
--
-- O CRUD de TipoIngresso pré-valida a unicidade do nome no service, mas é
-- preciso uma rede de segurança contra condição de corrida (duas requisições
-- POST simultâneas com o mesmo nome podem ambas passar na pré-validação e
-- então persistir, gerando duplicidade no tenant).
--
-- Decisão: índice UNIQUE composto (empresa_id, nome). Sintaxe portável padrão
-- SQL (suportada por MariaDB e PostgreSQL); identificadores não-quotados para
-- bater com a estratégia de nomenclatura do Hibernate adotada na V1.
--
-- Observação: a constraint cobre TODOS os registros (ativos e inativos). Se
-- houver necessidade futura de reaproveitar nomes de tipos soft-deletados,
-- migrar para um partial index (suportado em Postgres via
-- `WHERE ativo = true` — MariaDB não suporta partial indexes); por ora, a
-- regra de negócio é que o nome é exclusivo no histórico do tenant.

CREATE UNIQUE INDEX uk_tipoingresso_empresa_nome
    ON TipoIngresso (empresa_id, nome);
