package ka.mdo.service;

/**
 * Evento CDI disparado após uma {@code Notificacao} ser persistida pelo
 * {@code NotificacaoServiceImpl} (atividade 032). Consumido de forma
 * assíncrona pelos canais de entrega ({@code WebsocketChannel},
 * {@code EmailChannel}, {@code PushChannel}) via {@code @ObservesAsync}.
 *
 * <p>Apenas o id da notificação é propagado — cada canal carrega a entidade
 * em sua própria transação ({@code REQUIRES_NEW}) para evitar dependência
 * do contexto de persistência do chamador original. A falha de um canal não
 * impede os outros (cada listener roda em bean separado, com try/catch
 * interno que apenas loga).
 */
public record NotificacaoCriada(Long notificacaoId) {
}
