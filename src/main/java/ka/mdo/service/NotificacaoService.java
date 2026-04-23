package ka.mdo.service;

import ka.mdo.dto.NotificacaoResponseDTO;
import ka.mdo.model.Notificacao;
import ka.mdo.model.TipoNotificacao;

import java.util.List;

/**
 * Fachada de notificações (atividade 032).
 *
 * <p>Outros serviços de domínio — notadamente o fluxo de pendências da
 * atividade 031 — chamam {@link #enviar(Long, TipoNotificacao, String, String, String)}
 * para gerar uma notificação. A criação é síncrona (transação do chamador)
 * e a entrega por canal é assíncrona via evento CDI {@link NotificacaoCriada}.
 */
public interface NotificacaoService {

    /**
     * Persiste a notificação e dispara o evento de entrega. Retorna a
     * entidade criada (útil para testes). A transação é delegada à
     * implementação — o chamador não precisa abrir uma.
     *
     * @param destinatarioId id do usuário que deve receber a notificação.
     * @param tipo           categoria da notificação.
     * @param titulo         título curto (<= 150 chars).
     * @param mensagem       corpo da mensagem (<= 500 chars).
     * @param payloadJson    metadata opcional em JSON (pode ser {@code null}).
     */
    Notificacao enviar(Long destinatarioId,
                       TipoNotificacao tipo,
                       String titulo,
                       String mensagem,
                       String payloadJson);

    /**
     * Lista notificações do usuário logado (paginado).
     */
    List<NotificacaoResponseDTO> listarDoLogado(int pagina, int tamanho);

    /**
     * Marca uma notificação do logado como lida. Lança
     * {@code jakarta.ws.rs.NotFoundException} se não existir e
     * {@code jakarta.ws.rs.ForbiddenException} se não pertencer ao logado.
     */
    void marcarLida(Long id);

    /**
     * Conta notificações não lidas do logado (badge).
     */
    long contarNaoLidas();
}
