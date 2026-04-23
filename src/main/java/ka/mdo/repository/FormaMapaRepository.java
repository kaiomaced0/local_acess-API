package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.FormaMapa;

/**
 * Panache repository para {@link FormaMapa}. Operações em massa (substituir
 * todas as formas do mapa) são feitas via cascade/orphanRemoval no
 * {@code MapaEvento.formas}; mantemos o repository para leituras pontuais.
 */
@ApplicationScoped
public class FormaMapaRepository implements PanacheRepository<FormaMapa> {
}
