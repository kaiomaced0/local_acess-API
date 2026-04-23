package ka.mdo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request para PUT /api/v1/eventos/{id}/mapa — substitui o mapa do evento
 * por completo (upsert). As formas antigas são removidas e as novas criadas
 * (atividade 040).
 *
 * <p>{@code imagemFundoObjectKey} é opcional e geralmente NÃO vem no
 * payload: a imagem é enviada pelo endpoint separado
 * {@code POST /api/v1/eventos/{id}/mapa/imagem-fundo} que atualiza o campo
 * diretamente. Permitimos repassá-lo aqui para cenários de clonagem entre
 * eventos, mas o service valida se a chave referenciada existe.
 */
public record MapaEventoDTO(
        @NotNull @Min(1) @Max(100_000) Integer largura,
        @NotNull @Min(1) @Max(100_000) Integer altura,
        @Size(max = 20) String unidade,
        @Size(max = 500) String imagemFundoObjectKey,
        @NotNull @Valid List<FormaMapaDTO> formas
) {
}
