package ka.mdo.dto;

import ka.mdo.model.TipoIngresso;

/**
 * Response de {@link TipoIngresso} (atividade 015). Mantém o contrato mínimo
 * que o painel de gestão precisa — id, nome e flag de atividade.
 *
 * <p>Não expõe a empresa (vem do JWT, isolada pelo tenantFilter) nem a
 * lista de {@code EspacoEvento} (gerenciada pela atividade 030, com endpoint
 * próprio).
 */
public record TipoIngressoResponseDTO(
        Long id,
        String nome,
        Boolean ativo
) {
    public TipoIngressoResponseDTO(TipoIngresso t) {
        this(t.getId(), t.getNome(), t.getAtivo());
    }
}
