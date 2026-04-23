package ka.mdo.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import ka.mdo.model.CanalNotificacao;
import ka.mdo.model.Notificacao;
import ka.mdo.model.Usuario;
import ka.mdo.repository.NotificacaoRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canal de entrega por websocket (atividade 032).
 *
 * <p><b>Autenticação.</b> Optamos pela estratégia <i>AUTH por mensagem
 * inicial</i>: após {@code @OnOpen}, a conexão fica em estado "não
 * autenticada" — fechamos automaticamente se a primeira mensagem recebida
 * não for da forma {@code "AUTH <jwt>"}. Razões:
 * <ul>
 *     <li>Não depende da infra de segurança HTTP do Quarkus (handshake
 *     websocket em {@code quarkus-websockets} 3.5.3 não aplica
 *     {@code @RolesAllowed} por default).</li>
 *     <li>Evita passar o JWT por querystring, onde ele pode vazar em
 *     access-logs / referer.</li>
 *     <li>Simétrica com o fluxo HTTP — o mesmo JWT assinado por
 *     {@code TokenJwtService} é verificado aqui por {@link JWTParser}.</li>
 * </ul>
 *
 * <p>Nenhuma mensagem é roteada para o destinatário enquanto a sessão não
 * for autenticada. Após o AUTH, a sessão é indexada em {@link #SESSOES} por
 * {@code usuarioId} (subject do JWT) e o listener
 * {@link #entregar(NotificacaoCriada)} envia JSON quando a notificação for
 * para esse usuário.
 */
@ApplicationScoped
@ServerEndpoint("/api/v1/ws/notificacoes")
public class WebsocketChannel {

    private static final Logger LOG = Logger.getLogger(WebsocketChannel.class);

    /**
     * Sessões autenticadas indexadas pelo id do usuário. Um usuário pode
     * ter mais de uma aba aberta — guardamos a última; o caso simples basta
     * para a 032. Iterações futuras podem virar {@code Map<Long, Set<Session>>}.
     *
     * <p>Estático para sobreviver ao ciclo do bean; ConcurrentHashMap para
     * escrita/leitura concorrentes entre handlers websocket (threads do
     * container) e o listener assíncrono do CDI.
     */
    private static final Map<Long, Session> SESSOES = new ConcurrentHashMap<>();

    /** Chave usada em {@code Session#getUserProperties} para guardar o userId. */
    private static final String USER_ID_KEY = "usuarioId";

    @Inject
    JWTParser jwtParser;

    @Inject
    NotificacaoRepository notificacaoRepository;

    @OnOpen
    public void onOpen(Session session) {
        // Handshake aceito; aguardamos AUTH como primeira mensagem.
        LOG.debugf("WS aberto (sessionId=%s), aguardando AUTH", session.getId());
    }

    @OnMessage
    public void onMessage(String mensagem, Session session) {
        if (session.getUserProperties().get(USER_ID_KEY) != null) {
            // Já autenticado — no momento o canal é one-way (server->client).
            // Ignoramos mensagens subsequentes em vez de fechar, para
            // permitir pings do cliente sem derrubá-lo.
            return;
        }

        if (mensagem == null || !mensagem.startsWith("AUTH ")) {
            closeQuietly(session, "aguardando AUTH <token>");
            return;
        }

        String token = mensagem.substring(5).trim();
        if (token.isEmpty()) {
            closeQuietly(session, "token vazio");
            return;
        }

        try {
            JsonWebToken jwt = jwtParser.parse(token);
            String sub = jwt.getSubject();
            if (sub == null) {
                closeQuietly(session, "JWT sem subject");
                return;
            }
            Long usuarioId = Long.parseLong(sub);
            session.getUserProperties().put(USER_ID_KEY, usuarioId);
            SESSOES.put(usuarioId, session);
            LOG.infof("WS autenticado para usuário %d (sessionId=%s)", usuarioId, session.getId());
        } catch (ParseException | NumberFormatException e) {
            // Não logamos o token.
            LOG.warnf("WS AUTH falhou: %s", e.getMessage());
            closeQuietly(session, "JWT inválido");
        }
    }

    @OnClose
    public void onClose(Session session) {
        Object uid = session.getUserProperties().get(USER_ID_KEY);
        if (uid instanceof Long id) {
            // Só remove se a sessão guardada ainda for a mesma —
            // evita corrida entre reconexão e close tardio da sessão antiga.
            SESSOES.remove(id, session);
        }
    }

    @OnError
    public void onError(Session session, Throwable t) {
        LOG.warnf(t, "WS erro (sessionId=%s)", session == null ? "?" : session.getId());
    }

    /**
     * Listener do evento de notificação criada. Roda em transação nova
     * para carregar a entidade pelo id ({@code REQUIRES_NEW}) sem depender
     * do contexto da transação que criou a notificação.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void entregar(@ObservesAsync NotificacaoCriada evt) {
        try {
            Notificacao n = notificacaoRepository.findById(evt.notificacaoId());
            if (n == null) {
                LOG.warnf("WS entrega: notificação %d não encontrada", evt.notificacaoId());
                return;
            }
            Usuario destinatario = n.getDestinatario();
            if (destinatario == null || destinatario.getId() == null) {
                LOG.warnf("WS entrega: notificação %d sem destinatário", evt.notificacaoId());
                return;
            }

            // Respeita preferência do usuário por canal.
            if (destinatario.getCanaisNotificacao() == null
                    || !destinatario.getCanaisNotificacao().contains(CanalNotificacao.WEBSOCKET)) {
                return;
            }

            Session s = SESSOES.get(destinatario.getId());
            if (s == null || !s.isOpen()) {
                // Usuário offline: painel vai buscar via GET ao abrir.
                return;
            }

            String payload = montarJson(n);
            s.getAsyncRemote().sendText(payload);
        } catch (RuntimeException e) {
            LOG.errorf(e, "Falha ao entregar notificação %d via WS", evt.notificacaoId());
        }
    }

    /**
     * Formato simples (sem Jackson) para evitar dependência adicional e para
     * controlar escape de caracteres especiais manualmente.
     */
    private String montarJson(Notificacao n) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        sb.append("\"id\":").append(n.getId()).append(',');
        sb.append("\"tipo\":\"").append(n.getTipo().name()).append("\",");
        sb.append("\"titulo\":\"").append(escape(n.getTitulo())).append("\",");
        sb.append("\"mensagem\":\"").append(escape(n.getMensagem())).append("\",");
        sb.append("\"lida\":").append(n.isLida()).append(',');
        sb.append("\"criadaEm\":\"").append(n.getCriadaEm()).append('"');
        if (n.getPayloadJson() != null) {
            // payloadJson é JSON válido injetado pelo chamador; embutimos bruto
            // em campo separado. Se for malformado, o cliente reporta.
            sb.append(",\"payload\":").append(n.getPayloadJson());
        }
        sb.append('}');
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private void closeQuietly(Session session, String motivo) {
        try {
            LOG.debugf("Fechando WS (%s)", motivo);
            session.close();
        } catch (IOException ignored) {
            // best-effort
        }
    }

    /**
     * Apenas para facilitar testes: indica se há sessão autenticada para
     * um usuário. Evita expor o mapa.
     */
    static boolean temSessao(Long usuarioId) {
        Session s = SESSOES.get(usuarioId);
        return s != null && s.isOpen();
    }

}
