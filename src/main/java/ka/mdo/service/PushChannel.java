package ka.mdo.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ka.mdo.model.CanalNotificacao;
import ka.mdo.model.Notificacao;
import ka.mdo.model.Usuario;
import ka.mdo.repository.NotificacaoRepository;
import org.jboss.logging.Logger;

/**
 * Stub do canal push (FCM) — atividade 032 deixa a interface pronta mas
 * apenas loga. A integração real com Firebase Cloud Messaging fica para
 * iteração futura (ver nota em {@code 032-notificacoes.md}).
 *
 * <p>Presente aqui para que:
 * <ul>
 *     <li>Usuários que optarem por PUSH na preferência já vejam, via log,
 *     que a notificação foi gerada (útil para QA).</li>
 *     <li>A ativação futura seja simples substituição deste bean — nada
 *     no resto do sistema depende da implementação.</li>
 * </ul>
 */
@ApplicationScoped
public class PushChannel {

    private static final Logger LOG = Logger.getLogger(PushChannel.class);

    @Inject
    NotificacaoRepository notificacaoRepository;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void entregar(@ObservesAsync NotificacaoCriada evt) {
        try {
            Notificacao n = notificacaoRepository.findById(evt.notificacaoId());
            if (n == null) {
                return;
            }
            Usuario dest = n.getDestinatario();
            if (dest == null || dest.getCanaisNotificacao() == null
                    || !dest.getCanaisNotificacao().contains(CanalNotificacao.PUSH)) {
                return;
            }
            LOG.infof("PUSH pendente para usuário %d: %s", dest.getId(), n.getTitulo());
        } catch (RuntimeException e) {
            LOG.errorf(e, "Falha no PushChannel para notificação %d", evt.notificacaoId());
        }
    }
}
