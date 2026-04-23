package ka.mdo.dto;

import ka.mdo.model.EscopoGlobal;
import ka.mdo.model.Ingresso;

/**
 * Response de credencial. Nunca expõe o {@code token} opaco — apenas o
 * {@code id} (usado para gerar o QR em {@code /ingressos/{id}/qrcode}).
 *
 * @param escopoGlobal (033) {@code null} = credencial comum.
 *                     {@link EscopoGlobal#EMPRESA}/{@link EscopoGlobal#SUPER}
 *                     sinaliza credencial global — útil para gestores
 *                     visualizarem e auditarem.
 */
public record IngressoResponseDTO(
        Long id,
        String chaveAcesso,
        String lote,
        EscopoGlobal escopoGlobal
) {
    public IngressoResponseDTO(Ingresso i){
        this(i.getId(), i.getChaveAcesso(), i.getLote(), i.getEscopoGlobal());
    }
}
