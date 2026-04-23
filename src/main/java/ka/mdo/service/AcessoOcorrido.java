package ka.mdo.service;

import ka.mdo.model.ResultadoAcesso;
import ka.mdo.model.TipoMovimento;

import java.time.LocalDateTime;

/**
 * Evento CDI disparado por {@link AcessoService} após decidir o resultado de
 * uma validação. Consumido de forma assíncrona por
 * {@code LogAcessoService#registrar(AcessoOcorrido)} (via
 * {@code @ObservesAsync}) para persistir o {@code LogAcesso} sem bloquear a
 * resposta ao aparelho.
 *
 * <p>IDs são passados por valor para que o listener rode em sua própria
 * transação ({@code REQUIRES_NEW}) sem depender de entidades anexadas ao
 * contexto de persistência da requisição original.
 *
 * <p>Nunca inclui o token da credencial — apenas o id do ingresso (ou
 * {@code null} quando a credencial não foi encontrada).
 *
 * @param empresaId                id da empresa do aparelho (sempre presente).
 * @param aparelhoId               id do aparelho que fez a leitura (sempre presente).
 * @param ingressoId               id do {@code Ingresso}; {@code null} quando
 *                                 token inexistente.
 * @param localId                  id do {@code EspacoEvento}; {@code null} em
 *                                 aparelhos de entrada geral do evento.
 * @param resultado                decisão final da validação.
 * @param motivo                   código curto (mesmo devolvido ao aparelho);
 *                                 pode ser {@code null}.
 * @param dataHora                 instante da decisão (tirado em
 *                                 {@code AcessoService} para coincidir com o
 *                                 log/timeline do happy path).
 * @param fotoCapturadaObjectKey   chave do objeto no bucket
 *                                 {@code capturas-acesso} quando a validação
 *                                 foi via {@code /validar-com-foto} (atividade
 *                                 021). {@code null} para o fluxo clássico.
 * @param acessoGlobal             atividade 033: {@code true} quando a decisão
 *                                 foi tomada via credencial global (escopo
 *                                 {@code EMPRESA} ou {@code SUPER}). Destaque
 *                                 para auditoria — credenciais globais
 *                                 curto-circuitam parte das validações.
 * @param tipoMovimento            atividade 041: {@link TipoMovimento#ENTRADA}
 *                                 ou {@link TipoMovimento#SAIDA}. Default
 *                                 {@code ENTRADA} para leitores antigos.
 */
public record AcessoOcorrido(
        Long empresaId,
        Long aparelhoId,
        Long ingressoId,
        Long localId,
        ResultadoAcesso resultado,
        String motivo,
        LocalDateTime dataHora,
        String fotoCapturadaObjectKey,
        boolean acessoGlobal,
        TipoMovimento tipoMovimento
) {
    /** Factory conveniente para o fluxo sem foto (mantém call-sites antigos enxutos). */
    public static AcessoOcorrido semFoto(Long empresaId, Long aparelhoId, Long ingressoId,
                                         Long localId, ResultadoAcesso resultado, String motivo,
                                         LocalDateTime dataHora) {
        return new AcessoOcorrido(empresaId, aparelhoId, ingressoId, localId,
                resultado, motivo, dataHora, null, false, TipoMovimento.ENTRADA);
    }
}
