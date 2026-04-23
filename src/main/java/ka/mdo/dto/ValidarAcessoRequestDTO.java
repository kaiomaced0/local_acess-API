package ka.mdo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ka.mdo.model.TipoMovimento;

/**
 * Payload recebido do aparelho ao solicitar validação de um acesso.
 *
 * @param token           conteúdo do QR Code lido (= {@code Ingresso.token}).
 * @param localId         id do {@code EspacoEvento} onde o aparelho está; nulo
 *                        quando o aparelho opera na entrada do evento (sem
 *                        restrição por local interno).
 * @param aparelhoId      id do {@link ka.mdo.model.Aparelho} que está fazendo
 *                        a leitura. Serve para validar tenant + eventual
 *                        vínculo com evento/local.
 * @param tipoMovimento   opcional, default {@link TipoMovimento#ENTRADA}
 *                        (atividade 041). Leitores de saída enviam
 *                        {@link TipoMovimento#SAIDA} para alimentar o cálculo
 *                        de ocupação.
 */
public record ValidarAcessoRequestDTO(
        @NotBlank String token,
        Long localId,
        @NotNull Long aparelhoId,
        TipoMovimento tipoMovimento
) {

    /**
     * Compat com call-sites antigos (fluxo sem foto / aparelhos pre-041).
     * Preserva o default {@link TipoMovimento#ENTRADA}.
     */
    public ValidarAcessoRequestDTO(String token, Long localId, Long aparelhoId) {
        this(token, localId, aparelhoId, TipoMovimento.ENTRADA);
    }

    /**
     * Retorna o tipo de movimento aplicando o default {@link TipoMovimento#ENTRADA}
     * quando o campo não foi enviado pelo cliente.
     */
    public TipoMovimento tipoMovimentoOuDefault() {
        return tipoMovimento == null ? TipoMovimento.ENTRADA : tipoMovimento;
    }
}
