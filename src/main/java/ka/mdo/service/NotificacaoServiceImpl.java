package ka.mdo.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import ka.mdo.dto.NotificacaoResponseDTO;
import ka.mdo.model.Notificacao;
import ka.mdo.model.TipoNotificacao;
import ka.mdo.model.Usuario;
import ka.mdo.repository.NotificacaoRepository;
import ka.mdo.repository.UsuarioRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementação padrão de {@link NotificacaoService} (atividade 032).
 *
 * <p>Fluxo de {@link #enviar}:
 * <ol>
 *     <li>Carrega o destinatário; inferimos a empresa dele para o campo
 *     {@code Notificacao.empresa} (não usamos o JWT porque a notificação
 *     pode ser disparada por processos internos sem request ativo).</li>
 *     <li>Persiste dentro da transação do chamador (ou abre uma, via
 *     {@code @Transactional}).</li>
 *     <li>Dispara {@link NotificacaoCriada} com {@code Event#fireAsync} — a
 *     entrega por canal roda em outras threads.</li>
 * </ol>
 *
 * <p>Observação sobre multitenant: {@code Empresa} é derivado do usuário
 * destinatário. O chamador *nunca* passa empresaId.
 */
@ApplicationScoped
public class NotificacaoServiceImpl implements NotificacaoService {

    private static final Logger LOG = Logger.getLogger(NotificacaoServiceImpl.class);

    @Inject
    NotificacaoRepository notificacaoRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    Event<NotificacaoCriada> evento;

    @Inject
    JsonWebToken jwt;

    @Override
    @Transactional
    public Notificacao enviar(Long destinatarioId,
                              TipoNotificacao tipo,
                              String titulo,
                              String mensagem,
                              String payloadJson) {
        if (destinatarioId == null) {
            throw new IllegalArgumentException("destinatarioId é obrigatório");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("tipo é obrigatório");
        }

        Usuario destinatario = usuarioRepository.findById(destinatarioId);
        if (destinatario == null) {
            throw new NotFoundException("destinatário não encontrado: id=" + destinatarioId);
        }

        Notificacao n = new Notificacao();
        n.setEmpresa(destinatario.getEmpresa());
        n.setDestinatario(destinatario);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setPayloadJson(payloadJson);
        n.setLida(false);
        n.setCriadaEm(LocalDateTime.now());

        notificacaoRepository.persist(n);

        // fireAsync devolve CompletionStage; descartamos intencionalmente —
        // falha em canal NUNCA deve propagar para a transação que criou a
        // notificação. Cada listener trata suas exceções internamente.
        evento.fireAsync(new NotificacaoCriada(n.getId()));

        return n;
    }

    @Override
    public List<NotificacaoResponseDTO> listarDoLogado(int pagina, int tamanho) {
        Long usuarioId = usuarioDoJwt();
        int p = Math.max(0, pagina);
        int t = tamanho <= 0 ? 20 : Math.min(tamanho, 200);
        return notificacaoRepository.listarDoDestinatario(usuarioId, p, t).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void marcarLida(Long id) {
        Long usuarioId = usuarioDoJwt();
        Notificacao n = notificacaoRepository.findById(id);
        if (n == null) {
            throw new NotFoundException("notificação não encontrada");
        }
        if (n.getDestinatario() == null || !usuarioId.equals(n.getDestinatario().getId())) {
            // Não revelamos se existe em outro tenant — mesmo 403 do padrão
            // de outros resources.
            throw new ForbiddenException("notificação não pertence ao usuário logado");
        }
        if (!n.isLida()) {
            n.setLida(true);
        }
    }

    @Override
    public long contarNaoLidas() {
        return notificacaoRepository.contarNaoLidas(usuarioDoJwt());
    }

    private Long usuarioDoJwt() {
        // O subject do JWT emitido por TokenJwtService é o id do usuário.
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new ForbiddenException("JWT sem subject");
        }
        try {
            return Long.parseLong(sub);
        } catch (NumberFormatException e) {
            LOG.warnf("subject do JWT não é numérico: %s", sub);
            throw new ForbiddenException("JWT inválido");
        }
    }

    private NotificacaoResponseDTO toResponse(Notificacao n) {
        return new NotificacaoResponseDTO(
                n.getId(),
                n.getTipo(),
                n.getTitulo(),
                n.getMensagem(),
                n.getPayloadJson(),
                n.isLida(),
                n.getCriadaEm()
        );
    }
}
