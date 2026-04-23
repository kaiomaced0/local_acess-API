package ka.mdo.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ka.mdo.model.CanalNotificacao;
import ka.mdo.model.Notificacao;
import ka.mdo.model.Usuario;
import ka.mdo.repository.NotificacaoRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Canal de entrega por email (atividade 032). Usa {@link Mailer} do
 * {@code quarkus-mailer}.
 *
 * <p>Em dev o {@code quarkus.mailer.mock=true} (ver
 * {@code application.properties}) — nenhum email sai para rede, o conteúdo
 * vai ao log do MockMailbox. Em produção, configurar host/porta/credenciais
 * via env vars.
 *
 * <p>Falha de entrega nunca propaga — a notificação já está persistida e
 * visível no painel via websocket / GET.
 */
@ApplicationScoped
public class EmailChannel {

    private static final Logger LOG = Logger.getLogger(EmailChannel.class);

    @Inject
    Mailer mailer;

    @Inject
    NotificacaoRepository notificacaoRepository;

    @ConfigProperty(name = "notificacao.email.from")
    String from;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void entregar(@ObservesAsync NotificacaoCriada evt) {
        try {
            Notificacao n = notificacaoRepository.findById(evt.notificacaoId());
            if (n == null) {
                LOG.warnf("Email entrega: notificação %d não encontrada", evt.notificacaoId());
                return;
            }
            Usuario dest = n.getDestinatario();
            if (dest == null) {
                LOG.warnf("Email entrega: notificação %d sem destinatário", evt.notificacaoId());
                return;
            }
            if (dest.getCanaisNotificacao() == null
                    || !dest.getCanaisNotificacao().contains(CanalNotificacao.EMAIL)) {
                return;
            }
            String para = dest.getEmail();
            if (para == null || para.isBlank()) {
                LOG.warnf("Email entrega: usuário %d sem email", dest.getId());
                return;
            }

            Mail mail = Mail.withText(para, n.getTitulo(), n.getMensagem())
                    .setFrom(from);
            mailer.send(mail);
            LOG.infof("Email enviado para usuário %d (notificacao=%d)", dest.getId(), n.getId());
        } catch (RuntimeException e) {
            LOG.errorf(e, "Falha ao enviar email da notificação %d", evt.notificacaoId());
        }
    }
}
