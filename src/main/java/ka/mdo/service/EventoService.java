package ka.mdo.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.EspacoEventoDTO;
import ka.mdo.dto.EventoDTO;
import ka.mdo.dto.EventoResponseDTO;
import ka.mdo.model.EspacoEvento;
import ka.mdo.model.Evento;
import ka.mdo.repository.EspacoEventoRepository;
import ka.mdo.repository.EventoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class EventoService {

    @Inject
    EventoRepository repository;

    @Inject
    EspacoEventoRepository espacoEventoRepository;

    @Transactional
    public Response insert(EventoDTO eventoDTO) {
        try {
            Evento e = new Evento();
            e.setNome(eventoDTO.nome());
            e.setDescricao(eventoDTO.descricao());
            e.setLocal(eventoDTO.local());
            e.setInicioEvento(eventoDTO.inicioEvento());
            e.setFinalEvento(eventoDTO.finalEvento());
            List<EspacoEvento> espacoEventos = new ArrayList<>();
            e.setEspacoEventos(espacoEventos);
            repository.persist(e);
            Log.info("Requisição Evento.insert()");
            return Response.ok(new EventoResponseDTO(e)).build();
        } catch (Exception e) {
            Log.error("Erro ao rodar Requisição Evento.insert()" + e.getMessage());
            return Response.notModified(e.toString()).build();
        }
    }

    @Transactional
    public Response insertEspacoEvento(EspacoEventoDTO espacoEventoDTO) {
        try {
            Evento e = repository.findById(espacoEventoDTO.idEvento());
            EspacoEvento espacoEvento = new EspacoEvento();
            espacoEvento.setNome(espacoEventoDTO.nome());
            espacoEventoRepository.persist(espacoEvento);
            e.getEspacoEventos().add(espacoEvento);
            return Response.ok(new EventoResponseDTO(e)).build();
        } catch (Exception e) {
            return Response.notModified(e.toString()).build();
        }
    }


    public List<EventoResponseDTO> findAll() {
        return repository.listAll().stream().map(EventoResponseDTO::new).collect(Collectors.toList());
    }

    public EventoResponseDTO findById(Long id) {
        Evento u = repository
                .findById(id);
        if(u == null){
            return null;
        }
        return new EventoResponseDTO(u);
    }

    @Transactional
    public Evento update(Evento entity) {
        return repository.getEntityManager().merge(entity);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.findById(id).setAtivo(false);
    }
}
