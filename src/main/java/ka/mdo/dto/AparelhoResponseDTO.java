package ka.mdo.dto;

import ka.mdo.model.Aparelho;

import java.time.LocalDateTime;

/**
 * Response de {@link Aparelho} (atividade 014). Não expõe a referência completa
 * de {@code Empresa}, {@code Evento} ou {@code EspacoEvento} — apenas os IDs
 * relevantes para o painel do gestor.
 */
public record AparelhoResponseDTO(
        Long id,
        String descricao,
        Boolean ativo,
        Long eventoId,
        Long localEspecificoId,
        LocalDateTime dataInclusao
) {
    public AparelhoResponseDTO(Aparelho a) {
        this(
                a.getId(),
                a.getDescricao(),
                a.getAtivo(),
                a.getEvento() != null ? a.getEvento().getId() : null,
                a.getLocalEspecifico() != null ? a.getLocalEspecifico().getId() : null,
                a.getDataInclusao()
        );
    }
}
