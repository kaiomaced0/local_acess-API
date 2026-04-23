package ka.mdo.dto;

import java.util.List;

/**
 * Resposta do mapa de um evento. {@code imagemFundoUrl} é URL pré-assinada
 * via {@code StorageService} com TTL curto (null quando não há fundo).
 */
public record MapaEventoResponseDTO(
        Long id,
        Long eventoId,
        int largura,
        int altura,
        String unidade,
        String imagemFundoUrl,
        List<FormaMapaResponseDTO> formas
) {
}
