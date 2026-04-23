package ka.mdo.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.IngressoDTO;
import ka.mdo.dto.IngressoResponseDTO;
import ka.mdo.model.EscopoGlobal;
import ka.mdo.model.Ingresso;
import ka.mdo.model.Perfil;
import ka.mdo.model.TipoIngresso;
import ka.mdo.model.Usuario;
import ka.mdo.repository.EmpresaRepository;
import ka.mdo.repository.IngressoRepository;
import ka.mdo.repository.TipoIngressoRepository;
import ka.mdo.repository.UsuarioRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;

@ApplicationScoped
public class IngressoService {
    @Inject
    IngressoRepository repository;

    @Inject
    TipoIngressoRepository tipoIngressoRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    TokenService tokenService;

    @Inject
    JsonWebToken jwt;

    private Long empresaDoJwt() {
        Long empresaId = jwt.getClaim("empresaId");
        if (empresaId == null) {
            throw new ForbiddenException("JWT sem empresaId");
        }
        return empresaId;
    }

    @Transactional
    public Response adicionarIngresso(Long idUsuario, IngressoDTO dto) {
        Long empresaId = empresaDoJwt();

        Usuario u = usuarioRepository.findById(idUsuario);
        if (u == null || u.getEmpresa() == null || !u.getEmpresa().getId().equals(empresaId)) {
            throw new ForbiddenException("Usuário pertence a outra empresa");
        }

        TipoIngresso tipo = tipoIngressoRepository.findById(dto.idTipoIngresso());
        if (tipo == null || tipo.getEmpresa() == null || !tipo.getEmpresa().getId().equals(empresaId)) {
            throw new ForbiddenException("TipoIngresso pertence a outra empresa");
        }

        // Atividade 033: credenciais globais só podem ser emitidas por perfis
        // privilegiados. SUPER é cross-tenant, portanto restrito ao SUPER_ADMIN.
        // EMPRESA fica limitado a ADMIN_EMPRESA / SUPER_ADMIN.
        // Débito futuro (ver notas 033): 2FA obrigatório para emissão SUPER.
        if (dto.escopoGlobal() != null) {
            validarAutorizacaoEmissaoGlobal(dto.escopoGlobal());
        }

        Ingresso ingresso = new Ingresso();
        ingresso.setTipoIngresso(tipo);
        ingresso.setChaveAcesso(dto.chaveAcesso());
        ingresso.setEmpresa(empresaRepository.findById(empresaId));
        ingresso.setToken(tokenService.gerarToken());
        ingresso.setEscopoGlobal(dto.escopoGlobal());
        repository.persist(ingresso);
        u.getIngressos().add(ingresso);
        return Response.ok(new IngressoResponseDTO(ingresso)).build();
    }

    /**
     * Atividade 033: gate de emissão de credenciais globais.
     * <ul>
     *     <li>{@link EscopoGlobal#SUPER}: só {@code SUPER_ADMIN}.</li>
     *     <li>{@link EscopoGlobal#EMPRESA}: {@code ADMIN_EMPRESA} ou {@code SUPER_ADMIN}.</li>
     * </ul>
     */
    private void validarAutorizacaoEmissaoGlobal(EscopoGlobal escopo) {
        Set<String> grupos = jwt.getGroups();
        boolean superAdmin = grupos != null && grupos.contains(Perfil.SUPER_ADMIN.name());
        boolean adminEmpresa = grupos != null && grupos.contains(Perfil.ADMIN_EMPRESA.name());
        if (escopo == EscopoGlobal.SUPER && !superAdmin) {
            throw new ForbiddenException("Credencial global SUPER só pode ser emitida por SUPER_ADMIN");
        }
        if (escopo == EscopoGlobal.EMPRESA && !(superAdmin || adminEmpresa)) {
            throw new ForbiddenException("Credencial global EMPRESA exige ADMIN_EMPRESA ou SUPER_ADMIN");
        }
    }

    /**
     * Recupera o token de uma credencial após validar o acesso do chamador
     * (mesmo tenant + dono ou perfil de gestão). Usado para renderizar o QR
     * Code. Nunca exposto diretamente por um endpoint — apenas o QR deriva dele.
     */
    public String tokenParaQrCode(Long ingressoId) {
        Ingresso ingresso = repository.findById(ingressoId);
        if (ingresso == null) {
            throw new NotFoundException("Credencial não encontrada");
        }
        validarAcessoAoIngresso(ingresso);
        return ingresso.getToken();
    }

    private void validarAcessoAoIngresso(Ingresso ingresso) {
        Set<String> grupos = jwt.getGroups();
        boolean superAdmin = grupos != null && grupos.contains(Perfil.SUPER_ADMIN.name());

        // SUPER_ADMIN é cross-tenant; pula a verificação por empresa.
        if (!superAdmin) {
            Long empresaId = empresaDoJwt();
            if (ingresso.getEmpresa() == null
                    || !ingresso.getEmpresa().getId().equals(empresaId)) {
                throw new ForbiddenException("Credencial pertence a outra empresa");
            }
        }

        // CLIENTE só pode acessar o próprio ingresso. Demais perfis (gestores e
        // SUPER_ADMIN) passam direto — a restrição por tenant já foi aplicada.
        boolean clienteOnly = grupos != null
                && grupos.contains(Perfil.CLIENTE.name())
                && !grupos.contains(Perfil.ADMIN_EMPRESA.name())
                && !grupos.contains(Perfil.GESTOR_EVENTO.name())
                && !grupos.contains(Perfil.GESTOR_LOCAL.name())
                && !superAdmin;
        if (clienteOnly) {
            Long usuarioId = jwt.getClaim("usuarioId");
            if (usuarioId == null) {
                throw new ForbiddenException("JWT sem usuarioId");
            }
            Usuario dono = usuarioRepository.findByIngressoId(ingresso.getId());
            if (dono == null || !dono.getId().equals(usuarioId)) {
                throw new ForbiddenException("Credencial não pertence ao cliente");
            }
        }
    }

}
