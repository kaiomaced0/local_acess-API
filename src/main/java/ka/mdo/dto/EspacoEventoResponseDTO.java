package ka.mdo.dto;

import ka.mdo.model.EspacoEvento;

public record EspacoEventoResponseDTO(
        Long id, String nome
) {
    public EspacoEventoResponseDTO(EspacoEvento e){
        this(e.getId(), e.getNome());
    }
}

