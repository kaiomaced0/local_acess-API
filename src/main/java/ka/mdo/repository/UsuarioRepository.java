package ka.mdo.repository;

import ka.mdo.model.Usuario;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class UsuarioRepository implements PanacheRepository<Usuario> {

    public List<Usuario> findByNome(String nome){
        if (nome == null)
            return null;
        return find("UPPER(nome) LIKE ?1 ", "%"+nome.toUpperCase()+"%").list();
    }

    public Usuario findByEmailAndSenha(String email, String senha){
        if (email == null || senha == null)
            return null;

        return find("email = ?1 AND senha = ?2 ", email, senha).firstResult();
    }

    public Usuario findByCpfAndSenha(String cpf, String senha){
        if (cpf == null || senha == null)
            return null;

        return find("cpf = ?1 AND senha = ?2 ", cpf, senha).firstResult();
    }

    public Usuario findByCpf(String cpf){
        if (cpf == null)
            return null;

        return find("cpf = ?1", cpf).firstResult();
    }
    public Usuario findByIdModificado(String id){
        if (id == null)
            return null;

        return find("id = ?1", id).firstResult();
    }
    public Usuario findByEmail(String email){
        if (email == null)
            return null;

        return find("email = ?1", email).firstResult();
    }
}
