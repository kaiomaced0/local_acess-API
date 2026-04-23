package ka.mdo.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.EspacoEventoDTO;
import ka.mdo.dto.EventoDTO;
import ka.mdo.dto.EventoResponseDTO;
import ka.mdo.model.EspacoEvento;
import ka.mdo.model.Evento;
import ka.mdo.repository.EmpresaRepository;
import ka.mdo.repository.EspacoEventoRepository;
import ka.mdo.repository.EventoRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class EventoService {

    @Inject
    EventoRepository repository;

    @Inject
    EspacoEventoRepository espacoEventoRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    JsonWebToken jwt;

    private Long empresaDoJwt() {
        Long empresaId = jwt.getClaim("empresaId");
        if (empresaId == null) {
            throw new ForbiddenException("JWT sem empresaId");
        }
        return empresaId;
    }

    private void validarMesmoTenant(Evento evento) {
        if (evento == null || evento.getEmpresa() == null
                || !evento.getEmpresa().getId().equals(empresaDoJwt())) {
            throw new ForbiddenException("Recurso pertence a outra empresa");
        }
    }

    @Transactional
    public Response insert(EventoDTO eventoDTO) {
        try {
            Evento e = new Evento();
            e.setNome(eventoDTO.nome());
            e.setDescricao(eventoDTO.descricao());
            e.setLocal(eventoDTO.local());
            e.setInicioEvento(eventoDTO.inicioEvento());
            e.setFinalEvento(eventoDTO.finalEvento());
            e.setEmpresa(empresaRepository.findById(empresaDoJwt()));
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
            validarMesmoTenant(e);
            EspacoEvento espacoEvento = new EspacoEvento();
            espacoEvento.setNome(espacoEventoDTO.nome());
            espacoEvento.setEmpresa(e.getEmpresa());
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
        Evento existente = repository.findById(entity.getId());
        validarMesmoTenant(existente);
        entity.setEmpresa(existente.getEmpresa());
        return repository.getEntityManager().merge(entity);
    }

    @Transactional
    public void deleteById(Long id) {
        Evento evento = repository.findById(id);
        validarMesmoTenant(evento);
        evento.setAtivo(false);
    }
}
