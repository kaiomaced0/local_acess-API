package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.Aparelho;

@ApplicationScoped
public class AparelhoRepository implements PanacheRepository<Aparelho> {
}
