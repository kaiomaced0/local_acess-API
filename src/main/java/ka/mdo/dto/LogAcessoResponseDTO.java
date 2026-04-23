package ka.mdo.dto;

import ka.mdo.model.ResultadoAcesso;

import java.time.LocalDateTime;

/**
 * Projeção de {@link ka.mdo.model.LogAcesso} para respostas da API.
 *
 * <p>Intencionalmente NÃO expõe o token da credencial, nem dados pessoais
 * (nome, documento) — auditoria é por id. O descritivo do aparelho é útil
 * para operadores entenderem de onde veio a leitura sem precisar de outra
 * chamada.
 *
 * @param id                   id do log.
 * @param credencialId         id do {@code Ingresso} — pode ser {@code null}
 *                             quando o token lido não existia no tenant.
 * @param localId              id do {@code EspacoEvento} — {@code null} em
 *                             aparelhos de entrada geral do evento.
 * @param aparelhoId           id do {@code Aparelho} que fez a leitura.
 * @param aparelhoDescricao    descrição livre do aparelho (apelido).
 * @param resultado            {@link ResultadoAcesso} registrado.
 * @param motivo               código curto do motivo (mesmo devolvido ao
 *                             aparelho no momento da validação).
 * @param dataHora             instante em que a decisão foi tomada.
 * @param fotoCapturadaUrl     URL da foto (populada pela atividade 021).
 * @param acessoGlobal         (033) {@code true} quando a decisão foi tomada
 *                             via credencial global (escopo EMPRESA ou SUPER)
 *                             — curto-circuitou parte das validações.
 */
public record LogAcessoResponseDTO(
        Long id,
        Long credencialId,
        Long localId,
        Long aparelhoId,
        String aparelhoDescricao,
        ResultadoAcesso resultado,
        String motivo,
        LocalDateTime dataHora,
        String fotoCapturadaUrl,
        boolean acessoGlobal
) {
}
