package ka.mdo.model;

/**
 * Tipos de notificação do domínio. Persistido como STRING
 * ({@link jakarta.persistence.EnumType#STRING}) para estabilidade a novos valores.
 *
 * <p>Os primeiros quatro valores são específicos do fluxo de pendências /
 * credenciais (atividades 030 e 031) e {@code GERAL} cobre comunicações
 * avulsas (ex.: admin empresa enviando aviso para gestores).
 */
public enum TipoNotificacao {
    PENDENCIA_ABERTA,
    PENDENCIA_APROVADA,
    PENDENCIA_RECUSADA,
    CREDENCIAL_EMITIDA,
    GERAL
}
