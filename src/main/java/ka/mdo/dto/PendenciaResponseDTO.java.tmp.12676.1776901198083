package ka.mdo.dto;

import ka.mdo.model.StatusPendencia;

import java.time.LocalDateTime;

/**
 * Projeção de {@code Pendencia} para respostas REST (atividade 031).
 *
 * <ul>
 *     <li>Nunca expomos o token bruto — {@link #credencialTokenMascarado}
 *     devolve apenas os últimos 4 caracteres prefixados com {@code ***}.</li>
 *     <li>{@link #fotoCapturadaUrl} é uma URL pré-assinada (TTL curto) gerada
 *     sob demanda pelo service — o banco guarda só a chave do objeto.</li>
 *     <li>{@link #resolvidaPor} carrega apenas o {@code id} do usuário que
 *     resolveu (o nome pode ser pego em outra chamada se necessário).</li>
 * </ul>
 */
public record PendenciaResponseDTO(
        Long id,
        Long credencialId,
        String credencialTokenMascarado,
        Long localId,
        Long aparelhoId,
        String motivo,
        String fotoCapturadaUrl,
        StatusPendencia status,
        LocalDateTime criadaEm,
        LocalDateTime resolvidaEm,
        Long resolvidaPor
) {
}
