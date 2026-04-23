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

    /**
     * Quantos {@code TipoIngresso} estão na whitelist do local (atividade 030).
     * Evita forçar carga da coleção LAZY quando só queremos saber se está
     * vazia — a resposta "lista vazia = sem restrição" é decidida por esta
     * contagem antes de qualquer fetch.
     */
    public long contarTiposAutorizados(Long espacoId) {
        return getEntityManager().createQuery(
                        "SELECT COUNT(t) FROM EspacoEvento e JOIN e.tiposIngressoAutorizados t "
                                + "WHERE e.id = :espacoId", Long.class)
                .setParameter("espacoId", espacoId)
                .getSingleResult();
    }

    /**
     * True se o {@code tipoIngressoId} está na whitelist do {@code espacoId}
     * (atividade 030). Consulta direta em SQL — não depende da coleção LAZY
     * estar inicializada e não traz todos os tipos para memória.
     */
    public boolean contemTipoAutorizado(Long espacoId, Long tipoIngressoId) {
        Long count = getEntityManager().createQuery(
                        "SELECT COUNT(t) FROM EspacoEvento e JOIN e.tiposIngressoAutorizados t "
                                + "WHERE e.id = :espacoId AND t.id = :tipoId", Long.class)
                .setParameter("espacoId", espacoId)
                .setParameter("tipoId", tipoIngressoId)
                .getSingleResult();
        return count != null && count > 0;
    }
}
