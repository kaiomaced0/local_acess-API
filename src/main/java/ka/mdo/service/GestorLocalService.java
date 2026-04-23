package ka.mdo.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import ka.mdo.model.Empresa;
import ka.mdo.model.EspacoEvento;
import ka.mdo.model.GestorLocal;
import ka.mdo.model.Perfil;
import ka.mdo.model.Usuario;
import ka.mdo.repository.EspacoEventoRepository;
import ka.mdo.repository.GestorLocalRepository;
import ka.mdo.repository.UsuarioRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.Objects;
import java.util.Optional;

/**
 * Gerencia o vínculo {@link GestorLocal} (atividade 041).
 *
 * <p>Operações:
 * <ul>
 *     <li>{@link #vincular(Long, Long)} — idempotente: se o vínculo já existe,
 *     devolve o existente sem criar duplicata.</li>
 *     <li>{@link #desvincular(Long, Long)} — 404 se o vínculo não existia.</li>
 * </ul>
 *
 * <p>Autorização (controlada no resource): apenas {@code ADMIN_EMPRESA} e
 * {@code SUPER_ADMIN} podem criar/remover vínculos. Ambos usuário e local
 * precisam pertencer ao mesmo tenant do JWT.
 *
 * <p>Validação extra: o usuário vinculado precisa ter o perfil
 * {@link Perfil#GESTOR_LOCAL} — vincular qualquer perfil seria um erro
 * semântico silencioso; devolvemos {@code 400} via
 * {@link IllegalArgumentException} no caller.
 */
@ApplicationScoped
public class GestorLocalService {

    private static final Logger LOG = Logger.getLogger(GestorLocalService.class);

    @Inject
    GestorLocalRepository gestorLocalRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    EspacoEventoRepository espacoEventoRepository;

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
    public GestorLocal vincular(Long usuarioId, Long localId) {
        Long empresaJwt = empresaDoJwt();
        Usuario usuario = usuarioRepository.findById(usuarioId);
        if (usuario == null) {
            throw new NotFoundException("Usuario " + usuarioId + " não encontrado");
        }
        if (usuario.getEmpresa() == null
                || !Objects.equals(usuario.getEmpresa().getId(), empresaJwt)) {
            throw new ForbiddenException("Usuario pertence a outra empresa");
        }
        if (usuario.getPerfis() == null
                || !usuario.getPerfis().contains(Perfil.GESTOR_LOCAL)) {
            throw new IllegalArgumentException(
                    "Usuario " + usuarioId + " não tem perfil GESTOR_LOCAL — vínculo recusado");
        }

        EspacoEvento local = espacoEventoRepository.findById(localId);
        if (local == null) {
            throw new NotFoundException("EspacoEvento " + localId + " não encontrado");
        }
        if (local.getEmpresa() == null
                || !Objects.equals(local.getEmpresa().getId(), empresaJwt)) {
            throw new ForbiddenException("EspacoEvento pertence a outra empresa");
        }

        Optional<GestorLocal> existente =
                gestorLocalRepository.findByUsuarioELocal(usuarioId, localId);
        if (existente.isPresent()) {
            LOG.debugf("Vinculo gestor-local idempotente: usuario=%d local=%d",
                    usuarioId, localId);
            return existente.get();
        }

        Empresa empresa = usuario.getEmpresa();
        GestorLocal gl = new GestorLocal();
        gl.setGestor(usuario);
        gl.setLocal(local);
        gl.setEmpresa(empresa);
        gestorLocalRepository.persist(gl);
        LOG.infof("Vinculo gestor-local criado: usuario=%d local=%d empresa=%d",
                usuarioId, localId, empresa.getId());
        return gl;
    }

    @Transactional
    public void desvincular(Long usuarioId, Long localId) {
        empresaDoJwt();
        GestorLocal existente = gestorLocalRepository
                .findByUsuarioELocal(usuarioId, localId)
                .orElseThrow(() -> new NotFoundException(
                        "Vínculo usuario=" + usuarioId + " local=" + localId + " não encontrado"));
        // tenantFilter já isolou; checagem adicional para clareza.
        Long empresaJwt = jwt.getClaim("empresaId");
        if (empresaJwt != null
                && existente.getEmpresa() != null
                && !Objects.equals(existente.getEmpresa().getId(), empresaJwt)) {
            throw new ForbiddenException("Vínculo pertence a outra empresa");
        }
        gestorLocalRepository.delete(existente);
        LOG.infof("Vinculo gestor-local removido: usuario=%d local=%d",
                usuarioId, localId);
    }
}
