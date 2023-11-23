package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.EspacoEvento;

import java.util.List;

@ApplicationScoped
public class EspacoEventoRepository implements PanacheRepository<EspacoEvento> {

    public List<EspacoEvento> findByNome(String nome) {
        if (nome == null)
            return null;
        return find("UPPER(nome) LIKE ?1 ", "%" + nome.toUpperCase() + "%").list();
    }
}
