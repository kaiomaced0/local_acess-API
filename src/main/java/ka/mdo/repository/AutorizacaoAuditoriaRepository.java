package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.AutorizacaoAuditoria;

@ApplicationScoped
public class AutorizacaoAuditoriaRepository implements PanacheRepository<AutorizacaoAuditoria> {
}
