-- Atividade 033: credenciais globais (acesso a todos eventos/locais).
--
-- Ingresso.escopoGlobal
-- ---------------------
-- Flag opcional que transforma uma credencial comum em credencial global.
-- Valores permitidos (strings do enum ka.mdo.model.EscopoGlobal):
--   - EMPRESA : bypass de checagem de perfil/local dentro da própria empresa.
--   - SUPER   : cross-tenant, só emitida por SUPER_ADMIN.
-- NULL        : credencial comum (default). Fluxo clássico de validação.
ALTER TABLE Ingresso
    ADD COLUMN escopoGlobal VARCHAR(20) NULL;

-- LogAcesso.acessoGlobal
-- ----------------------
-- Destaque de auditoria: marca cada LogAcesso originado pelo curto-circuito
-- de credencial global, seja o resultado AUTORIZADO, NEGADO ou PENDENTE.
-- Default FALSE mantém compatibilidade com registros já existentes.
ALTER TABLE LogAcesso
    ADD COLUMN acessoGlobal BOOLEAN NOT NULL DEFAULT FALSE;
