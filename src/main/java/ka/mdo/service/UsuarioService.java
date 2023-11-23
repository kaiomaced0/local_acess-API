package ka.mdo.service;

import ka.mdo.dto.AuthUsuarioDTO;
import ka.mdo.dto.UsuarioDTO;
import ka.mdo.model.Perfil;
import ka.mdo.model.Usuario;
import ka.mdo.repository.UsuarioRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class UsuarioService {

    @Inject
    HashService hash;

    public Usuario byLoginAndSenha(AuthUsuarioDTO authDTO) {
        String senha = hash.getHashSenha(authDTO.senha());
        Usuario usuario = repository.findByEmailAndSenha(authDTO.login(), senha);
        if (usuario == null) {
            usuario = repository.findByCpfAndSenha(authDTO.login(), senha);
        }
        return usuario;
    }

    @Transactional
    public Response insert(UsuarioDTO usuarioDTO) {
        try {
            Log.info("Requisição Usuario.insert()");
            Usuario usuario = new Usuario();
            usuario.setCpf(usuarioDTO.cpf());
            usuario.setNome(usuarioDTO.nome());
            usuario.setEmail(usuarioDTO.email());
            usuario.setSenha(hash.getHashSenha(usuarioDTO.senha()));
            Set<Perfil> a = new HashSet<Perfil>();
            a.add(Perfil.USER);
            usuario.setPerfis(a);
            Usuario teste = repository.findByCpf(usuarioDTO.cpf());
            if (teste != null) {
                teste = repository.findByEmail(usuarioDTO.email());
            }
            if (usuarioDTO.getClass() == null || teste != null) {
                throw new Exception("Usuario nulo");
            }
            repository.persist(usuario);
            return Response.ok(usuario).build();
        } catch (Exception e) {
            Log.error("Erro ao rodar Requisição Usuario.insert()" + e.getMessage());
            return Response.notModified().build();
        }
    }

    @Inject
    UsuarioRepository repository;

    public List<Usuario> findAll() {
        return repository.listAll();
    }

    public Usuario findById(Long id) {
        return repository
                .findById(id);
    }

    @Transactional
    public Usuario create(Usuario entity) {
        Usuario t = entity;
        repository.persist(t);
        return entity;
    }

    @Transactional
    public Usuario update(Usuario entity) {
        return repository.getEntityManager().merge(entity);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.findById(id).setAtivo(false);
    }
}
