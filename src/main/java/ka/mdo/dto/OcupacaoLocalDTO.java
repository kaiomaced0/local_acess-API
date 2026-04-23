package ka.mdo.dto;

/**
 * Ocupação atual (pessoas dentro) de um {@link ka.mdo.model.EspacoEvento}
 * do evento consultado. Atividade 041.
 *
 * <p>Calculada como
 * {@code COUNT(LogAcesso AUTORIZADO ENTRADA) - COUNT(LogAcesso AUTORIZADO SAIDA)}
 * para o local, sem recorte por janela (a tabela {@code LogAcesso} já é
 * tenant-filtered). Pode ficar {@code 0} quando o local ainda não teve
 * leituras ou quando as saídas bateram as entradas.
 */
public record OcupacaoLocalDTO(
        Long localId,
        String localNome,
        long pessoasDentro
) {
}
