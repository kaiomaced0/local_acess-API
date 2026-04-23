package ka.mdo.model;

/**
 * Resultado de uma tentativa de validação de acesso feita por um
 * {@link Aparelho} sobre uma credencial ({@link Ingresso}).
 *
 * <ul>
 *     <li>{@link #AUTORIZADO}: todas as validações passaram — abrir catraca.</li>
 *     <li>{@link #PENDENTE}: a decisão depende de intervenção humana ou
 *     completar dados (validação facial — atividade 021, dados pessoais
 *     faltantes — atividade 020).</li>
 *     <li>{@link #NEGADO}: falhou uma regra dura (credencial inexistente,
 *     fora do período do evento, aparelho inativo ou de outro tenant,
 *     tipo de ingresso sem permissão no local).</li>
 * </ul>
 */
public enum ResultadoAcesso {
    AUTORIZADO,
    PENDENTE,
    NEGADO
}
