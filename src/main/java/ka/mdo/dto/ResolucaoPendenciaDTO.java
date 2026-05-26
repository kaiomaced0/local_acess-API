package ka.mdo.dto;

import jakarta.validation.constraints.Size;

/**
 * Body opcional dos endpoints {@code POST /pendencias/{id}/aprovar} e
 * {@code POST /pendencias/{id}/recusar} (atividade 031).
 *
 * <p>A observação é persistida em {@code Pendencia.observacaoResolucao} e fica
 * disponível no histórico — útil para auditoria (ex.: "cliente apresentou RG
 * físico, liberado").
 */
public record ResolucaoPendenciaDTO(
        @Size(max = 500, message = "observação deve ter no máximo 500 caracteres")
        String observacao
) {
}
