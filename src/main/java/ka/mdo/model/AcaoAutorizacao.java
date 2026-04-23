package ka.mdo.model;

/**
 * Ação registrada em {@link AutorizacaoAuditoria} quando a whitelist de
 * {@link TipoIngresso} de um {@link EspacoEvento} muda (atividade 030).
 */
public enum AcaoAutorizacao {
    /** Um TipoIngresso específico foi adicionado à whitelist. */
    ADICIONADO,
    /** Um TipoIngresso específico foi removido da whitelist. */
    REMOVIDO,
    /** A lista inteira foi substituída (PUT). {@code tipoIngressoId} fica nulo. */
    SUBSTITUIDO
}
