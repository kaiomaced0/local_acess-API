package ka.mdo.dto;

import ka.mdo.model.TipoNotificacao;

import java.time.LocalDateTime;

/**
 * Projeção de {@code Notificacao} para respostas REST (atividade 032).
 * A entidade nunca é serializada diretamente — apenas ids e campos seguros
 * são expostos.
 */
public record NotificacaoResponseDTO(
        Long id,
        TipoNotificacao tipo,
        String titulo,
        String mensagem,
        String payloadJson,
        boolean lida,
        LocalDateTime criadaEm
) {
}
