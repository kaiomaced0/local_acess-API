package ka.mdo.dto;

import ka.mdo.model.EscopoGlobal;

/**
 * Request para emissão de credencial.
 *
 * @param escopoGlobal opcional — quando informado, emite uma credencial
 *                     global (atividade 033). {@link EscopoGlobal#EMPRESA}
 *                     requer {@code ADMIN_EMPRESA} ou {@code SUPER_ADMIN};
 *                     {@link EscopoGlobal#SUPER} requer {@code SUPER_ADMIN}.
 *                     Default {@code null} = credencial comum.
 */
public record IngressoDTO(
        String chaveAcesso,
        String lote,
        Long idTipoIngresso,
        EscopoGlobal escopoGlobal
) {
}
