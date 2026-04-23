-- V4: renomeia valores legados de Perfil para os novos nomes formais.
--
-- Contexto: até a atividade 002 o enum `Perfil` continha ADMIN/USER/MEDICO
-- (e um `PerfilConverter` tentava persistir como Integer, o que divergia do
-- schema VARCHAR(30) de `usuario_perfil.perfil`). Após a 002 o enum passa a
-- ser persistido como STRING (nome do enum) e os valores formais passam a ser:
--   SUPER_ADMIN, ADMIN_EMPRESA, GESTOR_EVENTO, GESTOR_LOCAL,
--   OPERADOR_APARELHO, CLIENTE.
--
-- Mapeamentos aplicados (best-effort) para registros herdados:
--   ADMIN  -> ADMIN_EMPRESA   (Admin de empresa é o mais próximo do antigo ADMIN)
--   USER   -> CLIENTE         (usuário comum = cliente final)
--   MEDICO -> CLIENTE         (MEDICO não existe no novo domínio; degradado para CLIENTE)
--   Labels legados ("Admin", "User", "Medico") igualmente migrados.
--
-- Idempotente: UPDATE com WHERE. Se não houver linhas legadas, não faz nada.

UPDATE usuario_perfil SET perfil = 'ADMIN_EMPRESA' WHERE perfil IN ('ADMIN', 'Admin', '1');
UPDATE usuario_perfil SET perfil = 'CLIENTE'       WHERE perfil IN ('USER',  'User',  '2');
UPDATE usuario_perfil SET perfil = 'CLIENTE'       WHERE perfil IN ('MEDICO','Medico','3');
