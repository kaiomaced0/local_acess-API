package ka.mdo.model;

/**
 * Escopo de uma credencial global (atividade 033). Credenciais globais
 * curto-circuitam checagens de perfil/local no {@code AcessoService}.
 *
 * <ul>
 *     <li>{@link #EMPRESA} — bypass de checagem de perfil/local e de
 *     autorização por {@link TipoIngresso}, mas mantém as demais regras
 *     dentro da mesma empresa (período do evento, dados pessoais, facial).
 *     Emitido por {@code ADMIN_EMPRESA} ou {@code SUPER_ADMIN}.</li>
 *     <li>{@link #SUPER} — cross-tenant: pula TODAS as validações
 *     subsequentes (tenant, período, perfil, dados pessoais, facial).
 *     Somente {@code SUPER_ADMIN} pode emitir. Débito técnico: exigir 2FA
 *     antes da emissão (não implementado aqui).</li>
 * </ul>
 *
 * <p>Persistido como {@code VARCHAR(20) NULL} em {@code Ingresso.escopoGlobal}.
 * Valor {@code null} representa credencial NÃO-global (fluxo clássico).
 */
public enum EscopoGlobal {
    EMPRESA,
    SUPER
}
