package ka.mdo.dto;

import ka.mdo.model.Evento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record EventoResponseDTO(
        Long id,
        String nome,
        String descricao,
        String local,
        LocalDateTime inicioEvento,
        LocalDateTime finalEvento,
        List<EspacoEventoResponseDTO> espacoEventosResponseDTO
) {
    public EventoResponseDTO(Evento evento){
        this(evento.getId(), evento.getNome(), evento.getDescricao(), evento.getLocal(), evento.getInicioEvento(),evento.getFinalEvento(), evento.getEspacoEventos().stream().map(EspacoEventoResponseDTO::new).collect(Collectors.toList()));
    }
}
