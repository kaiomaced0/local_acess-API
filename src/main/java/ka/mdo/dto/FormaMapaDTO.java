package ka.mdo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ka.mdo.model.TipoForma;

import java.util.Map;

/**
 * Uma forma geométrica do mapa 2D (atividade 040).
 *
 * <p>{@code geometria} é um mapa livre que é serializado como JSON
 * antes de persistir. Estrutura esperada por tipo:
 * <ul>
 *   <li>RETANGULO: {@code {"x":10,"y":20,"w":100,"h":50}}</li>
 *   <li>CIRCULO:   {@code {"cx":50,"cy":50,"r":30}}</li>
 *   <li>POLIGONO:  {@code {"pontos":[[x1,y1],[x2,y2],...]}}</li>
 * </ul>
 *
 * <p>Validação estrutural (chaves presentes e valores numéricos) é feita
 * em {@code MapaEventoService.validarGeometria}. A cor aceita só o formato
 * HTML curto {@code #RRGGBB}.
 */
public record FormaMapaDTO(
        @NotNull Long espacoId,
        @NotNull TipoForma tipo,
        @NotNull Map<String, Object> geometria,
        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "corHex deve estar no formato #RRGGBB")
        String corHex,
        @Size(max = 100)
        String rotulo
) {
}
