package ka.mdo.model;

/**
 * Canal de entrega de uma {@link Notificacao}. Cada usuário possui uma
 * {@code Set<CanalNotificacao>} indicando em quais canais deseja receber
 * notificações. Persistido como STRING em {@code usuario_canais_notificacao}.
 *
 * <p>{@code PUSH} é stub nesta primeira iteração (atividade 032) — o
 * {@code PushChannel} apenas loga. FCM real entra em iteração futura.
 */
public enum CanalNotificacao {
    WEBSOCKET,
    EMAIL,
    PUSH
}
