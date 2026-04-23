package ka.mdo.dto;

import ka.mdo.model.EspacoEvento;
import ka.mdo.model.TipoIngresso;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resposta da listagem/manipulação da whitelist de tipos de ingresso
 * autorizados em um {@link EspacoEvento} (atividade 030).
 *
 * <p>Nunca serializa a entidade {@link TipoIngresso} diretamente; projeta
 * em {@link TipoIngressoResumo} (id + nome).
 */
public record AutorizacaoResponseDTO(
        Long espacoId,
        String espacoNome,
        List<TipoIngressoResumo> tiposIngressoAutorizados
) {
    public static AutorizacaoResponseDTO from(EspacoEvento espaco, Set<TipoIngresso> tipos) {
        List<TipoIngressoResumo> resumos = tipos.stream()
                .map(t -> new TipoIngressoResumo(t.getId(), t.getNome()))
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .collect(Collectors.toList());
        return new AutorizacaoResponseDTO(espaco.getId(), espaco.getNome(), resumos);
    }

    public record TipoIngressoResumo(Long id, String nome) {
    }
}
