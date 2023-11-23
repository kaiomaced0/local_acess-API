package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.Ingresso;
import ka.mdo.model.TipoIngresso;

import java.util.List;

@ApplicationScoped
public class TipoIngressoRepository implements PanacheRepository<TipoIngresso> {

    public List<TipoIngresso> findByNome(String nome) {
        if (nome == null)
            return null;
        return find("UPPER(nome) LIKE ?1 ", "%" + nome.toUpperCase() + "%").list();
    }
}
