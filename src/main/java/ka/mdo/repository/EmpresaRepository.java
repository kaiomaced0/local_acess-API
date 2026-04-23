package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.Empresa;

@ApplicationScoped
public class EmpresaRepository implements PanacheRepository<Empresa> {

    public Empresa findByCnpj(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        return find("cnpj = ?1", cnpj).firstResult();
    }
}
