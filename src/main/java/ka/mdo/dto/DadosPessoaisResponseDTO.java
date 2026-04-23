package ka.mdo.dto;

import java.time.LocalDate;

import ka.mdo.model.TipoDocumento;

/**
 * Resposta com dados pessoais do usuário.
 *
 * <p>O campo {@code documentoMascarado} contém o documento ofuscado — ex.
 * {@code ***.***.***-12} para CPF. O documento completo só é devolvido pelo
 * endpoint dedicado de gestor; nunca é incluído nesta resposta default.
 *
 * <p>{@code fotoUrl} e {@code documentoFotoUrl} são URLs pré-assinadas com
 * TTL curto geradas a cada chamada (ver {@code StorageService.downloadUrl}).
 * Quando a imagem ainda não foi enviada, os campos vêm {@code null}.
 */
public record DadosPessoaisResponseDTO(
        Long id,
        String nomeCompleto,
        TipoDocumento tipoDocumento,
        String documentoMascarado,
        LocalDate dataNascimento,
        String fotoUrl,
        String documentoFotoUrl
) {
}
