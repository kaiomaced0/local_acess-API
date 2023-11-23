package ka.mdo.dto;

import ka.mdo.model.Ingresso;

public record IngressoResponseDTO(
        Long id, String chaveAcesso, String lote
) {
    public IngressoResponseDTO(Ingresso i){
        this(i.getId(), i.getChaveAcesso(), i.getLote());
    }
}
