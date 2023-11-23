package ka.mdo.dto;

public record MudarSenhaDTO(
        String login,
        String senhaAntiga,
        String novaSenha
) {
}
