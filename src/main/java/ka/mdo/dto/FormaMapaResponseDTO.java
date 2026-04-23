package ka.mdo.dto;

import ka.mdo.model.TipoForma;

import java.util.Map;

/**
 * Forma retornada no GET do mapa. {@code geometria} chega desserializada
 * de volta para {@code Map<String,Object>} para que o frontend trabalhe
 * diretamente com o objeto (sem reparse de string JSON).
 */
public record FormaMapaResponseDTO(
        Long id,
        Long espacoId,
        String espacoNome,
        TipoForma tipo,
        Map<String, Object> geometria,
        String corHex,
        String rotulo
) {
}
