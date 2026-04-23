package ka.mdo.model;

/**
 * Estados possíveis de uma {@link Pendencia} de acesso (atividade 031).
 *
 * <ul>
 *     <li>{@link #ABERTA} — default ao criar; aguardando revisão do gestor.</li>
 *     <li>{@link #APROVADA} — gestor liberou manualmente o acesso.</li>
 *     <li>{@link #RECUSADA} — gestor bloqueou.</li>
 * </ul>
 *
 * <p>Persistido como STRING ({@link jakarta.persistence.EnumType#STRING}) para
 * manter estabilidade se valores novos forem adicionados depois.
 */
public enum StatusPendencia {
    ABERTA,
    APROVADA,
    RECUSADA
}
