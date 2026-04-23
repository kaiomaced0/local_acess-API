package ka.mdo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.MapaEvento;

import java.util.Optional;

/**
 * Panache repository para {@link MapaEvento}. As consultas são filtradas
 * automaticamente pelo {@code tenantFilter} (anotado em
 * {@link MapaEvento}).
 */
@ApplicationScoped
public class MapaEventoRepository implements PanacheRepository<MapaEvento> {

    /**
     * Um evento tem no máximo um mapa (UNIQUE em evento_id). Usa LIMIT 1
     * para robustez em caso de migração inconsistente.
     */
    public Optional<MapaEvento> findByEventoId(Long eventoId) {
        return find("evento.id", eventoId).firstResultOptional();
    }
}
