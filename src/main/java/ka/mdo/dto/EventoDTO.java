package ka.mdo.dto;

import java.time.LocalDateTime;

public record EventoDTO(
        String nome,
        String descricao,
        String local,
        LocalDateTime inicioEvento,
        LocalDateTime finalEvento
) {
}
