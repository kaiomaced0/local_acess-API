package ka.mdo.service;

import ka.mdo.dto.MudarSenhaDTO;
import ka.mdo.dto.UsuarioResponseDTO;
import ka.mdo.model.Usuario;
import ka.mdo.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class UsuarioLogadoService {

    @Inject
    JsonWebToken jsonWebToken;

    @Inject
    UsuarioRepository usuarioRepository;
//    @Inject
//    SecurityContext securityContext;
    @Inject
    HashService hash;

    @Transactional
    public UsuarioResponseDTO updateSenha(MudarSenhaDTO senha) {
        try {

            Usuario entity = usuarioRepository.findByIdModificado(jsonWebToken.getSubject());

            if(hash.getHashSenha(senha.senhaAntiga()) != entity.getSenha())
                throw new Exception("Senha anterior Incorreta");

            entity.setSenha(hash.getHashSenha(senha.novaSenha()));
            return new UsuarioResponseDTO(entity);
        } catch (Exception e) {
            return null;
        }

    }

}



