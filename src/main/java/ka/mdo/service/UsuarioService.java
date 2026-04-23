package ka.mdo.service;

import ka.mdo.dto.*;
import ka.mdo.model.Empresa;
import ka.mdo.model.Perfil;
import ka.mdo.model.Usuario;
import ka.mdo.repository.EmpresaRepository;
import ka.mdo.repository.IngressoRepository;
import ka.mdo.repository.TipoIngressoRepository;
import ka.mdo.repository.UsuarioRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class UsuarioService {

    @Inject
    HashService hash;

    @Inject
    UsuarioRepository repository;

    @Inject
    TipoIngressoRepository tipoIngressoRepository;

    @Inject
    IngressoRepository ingressoRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    JsonWebToken jwt;

    private Long empresaDoJwtOpcional() {
        try {
            return jwt.getClaim("empresaId");
        } catch (Exception e) {
            return null;
        }
    }

    private void validarMesmoTenant(Usuario usuario) {
        Long empresaId = empresaDoJwtOpcional();
        if (empresaId == null) {
            throw new ForbiddenException("JWT sem empresaId");
        }
        if (usuario == null || usuario.getEmpresa() == null
                || !usuario.getEmpresa().getId().equals(empresaId)) {
            throw new ForbiddenException("Recurso pertence a outra empresa");
        }
    }

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
            Long empresaId = empresaDoJwtOpcional();
            if (empresaId == null) {
                throw new ForbiddenException("Cadastro de usuário exige JWT com empresaId");
            }
            Empresa empresa = empresaRepository.findById(empresaId);
            if (empresa == null) {
                throw new ForbiddenException("Empresa do JWT não encontrada");
            }

            Usuario usuario = new Usuario();
            usuario.setCpf(usuarioDTO.cpf());
            usuario.setNome(usuarioDTO.nome());
            usuario.setEmail(usuarioDTO.email());
            usuario.setSenha(hash.getHashSenha(usuarioDTO.senha()));
            usuario.setEmpresa(empresa);
            Set<Perfil> a = new HashSet<Perfil>();
            a.add(Perfil.CLIENTE);
            usuario.setPerfis(a);
            Usuario teste = repository.findByCpf(usuarioDTO.cpf());
            if (teste != null) {
                teste = repository.findByEmail(usuarioDTO.email());
            }
            if (usuarioDTO.getClass() == null || teste != null) {
                throw new Exception("Já existe usuario com esse cpf e email");
            }
            repository.persist(usuario);
            return Response.ok(usuario).build();
        } catch (Exception e) {
            Log.error("Erro ao rodar Requisição Usuario.insert()" + e.getMessage());
            return Response.notModified(e.toString()).build();
        }
    }

    public List<UsuarioResponseDTO> findAll() {
        return repository.listAll().stream().map(UsuarioResponseDTO::new).collect(Collectors.toList());
    }

    public UsuarioResponseDTO findById(Long id) {
        Usuario u = repository
                .findById(id);
        if(u == null){
            return null;
        }
        return new UsuarioResponseDTO(u);
    }

    @Transactional
    public Usuario create(Usuario entity) {
        Usuario t = entity;
        repository.persist(t);
        return entity;
    }

    @Transactional
    public Usuario update(Usuario entity) {
        Usuario existente = repository.findById(entity.getId());
        validarMesmoTenant(existente);
        entity.setEmpresa(existente.getEmpresa());
        return repository.getEntityManager().merge(entity);
    }

    @Transactional
    public void deleteById(Long id) {
        Usuario usuario = repository.findById(id);
        validarMesmoTenant(usuario);
        usuario.setAtivo(false);
    }
}
