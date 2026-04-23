package ka.mdo.dto;

import ka.mdo.model.ResultadoAcesso;

/**
 * Resposta devolvida ao aparelho após a validação de uma credencial.
 *
 * @param resultado             decisão final ({@link ResultadoAcesso}).
 * @param motivo                código/descrição curta do motivo. Útil para
 *                              debug no aparelho e para o log de acesso
 *                              (atividade 012).
 * @param credencialId          id do {@link ka.mdo.model.Ingresso} quando
 *                              localizado; {@code null} se a credencial
 *                              não foi encontrada pelo token.
 * @param exigeValidacaoFacial  se a próxima etapa depende de validação
 *                              facial. Hoje sempre {@code false};
 *                              atividade 021 passará a popular.
 */
public record ValidarAcessoResponseDTO(
        ResultadoAcesso resultado,
        String motivo,
        Long credencialId,
        boolean exigeValidacaoFacial
) {
}
