package ka.mdo.model;

/**
 * Estados administrativos do tenant {@code Empresa}.
 *
 * <ul>
 *   <li>{@code ATIVA} — operação normal. Único estado em que usuários da
 *       empresa conseguem autenticar (ver {@code UsuarioService.byLoginAndSenha}).</li>
 *   <li>{@code SUSPENSA} — temporariamente bloqueada (ex.: inadimplência).
 *       Pode voltar para {@code ATIVA} ou ser {@code ENCERRADA}.</li>
 *   <li>{@code ENCERRADA} — estado final. Adicionado na atividade 008;
 *       não permite transição de volta.</li>
 * </ul>
 */
public enum StatusEmpresa {
    ATIVA,
    SUSPENSA,
    ENCERRADA
}
