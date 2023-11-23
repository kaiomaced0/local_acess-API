package ka.mdo.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.IngressoDTO;
import ka.mdo.dto.IngressoResponseDTO;
import ka.mdo.model.Ingresso;
import ka.mdo.model.Usuario;
import ka.mdo.repository.IngressoRepository;
import ka.mdo.repository.TipoIngressoRepository;
import ka.mdo.repository.UsuarioRepository;

@ApplicationScoped
public class IngressoService {
    @Inject
    IngressoRepository repository;

    @Inject
    TipoIngressoRepository tipoIngressoRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Transactional
    public Response adicionarIngresso(Long idUsuario, IngressoDTO dto){
        Ingresso ingresso = new Ingresso();
        ingresso.setTipoIngresso(tipoIngressoRepository.findById(dto.idTipoIngresso()));
        ingresso.setChaveAcesso(dto.chaveAcesso());
        repository.persist(ingresso);
        Usuario u = usuarioRepository.findById(idUsuario);
        u.getIngressos().add(ingresso);
        return Response.ok(new IngressoResponseDTO(ingresso)).build();
    }

}
