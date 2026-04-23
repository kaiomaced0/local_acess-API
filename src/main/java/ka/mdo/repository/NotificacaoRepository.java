package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.Notificacao;

import java.util.List;

/**
 * Repositório de {@link Notificacao}. O filtro multi-tenant do Hibernate
 * isola por empresa automaticamente — as queries não dependem de
 * {@code empresaId} vindo do cliente. Filtros por {@code destinatario_id}
 * são aplicados explicitamente pelo {@code NotificacaoService} já que cada
 * usuário só enxerga as próprias notificações.
 */
@ApplicationScoped
public class NotificacaoRepository implements PanacheRepository<Notificacao> {

    /**
     * Lista notificações de um destinatário paginadas (mais recentes primeiro).
     */
    public List<Notificacao> listarDoDestinatario(Long destinatarioId, int pageIndex, int pageSize) {
        return find("destinatario.id = ?1",
                Sort.by("criadaEm").descending(),
                destinatarioId)
                .page(Page.of(pageIndex, pageSize))
                .list();
    }

    /**
     * Conta rápida de não-lidas — serve para o badge do painel.
     */
    public long contarNaoLidas(Long destinatarioId) {
        return count("destinatario.id = ?1 AND lida = false", destinatarioId);
    }
}
