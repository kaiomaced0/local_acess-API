package ka.mdo.service;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ka.mdo.model.Empresa;
import ka.mdo.model.Perfil;
import ka.mdo.model.StatusEmpresa;
import ka.mdo.model.Usuario;
import ka.mdo.repository.EmpresaRepository;
import ka.mdo.repository.UsuarioRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * Cria o primeiro {@link Perfil#SUPER_ADMIN} na subida do app, se ainda não
 * existir nenhum no banco. Evita que a instância fique inacessível para
 * operações cross-tenant (criar empresas, etc.).
 *
 * <p>Motivo para usar {@link StartupEvent} em vez de migração SQL:
 * o hash de senha depende do {@link HashService} (PBKDF2 com sal específico
 * do app). Gerar o hash em uma migração Flyway exigiria replicar a lógica em
 * SQL ou hardcodar um hash — quebra ao trocar o sal/iterações. Aqui usamos
 * o próprio serviço e mantemos uma única fonte de verdade.</p>
 *
 * <p>O SUPER_ADMIN bootstrap <b>não</b> pertence a nenhuma empresa (campo
 * {@code empresa} fica nulo nessa conta). Consequentemente o JWT emitido no
 * login dele não terá claim {@code empresaId} — o {@link ka.mdo.tenant.TenantRequestFilter}
 * abre exceção explícita para SUPER_ADMIN sem empresaId, permitindo que ele
 * cadastre as primeiras empresas. Como a coluna {@code empresa_id} é
 * {@code NOT NULL} nas tabelas de negócio (migração V3), o SUPER_ADMIN precisa
 * ser anexado a uma empresa-sistema antes de gravar — por isso a conta é
 * associada à "Empresa Padrão" (id=1) que já existe desde a V3 apenas para
 * satisfazer a integridade referencial. A semântica cross-tenant continua
 * garantida pelo filtro (que não é ativado para SUPER_ADMIN).</p>
 */
@ApplicationScoped
public class BootstrapService {

    @ConfigProperty(name = "bootstrap.super-admin.email", defaultValue = "admin@local-acess.local")
    String superAdminEmail;

    @ConfigProperty(name = "bootstrap.super-admin.senha", defaultValue = "admin123")
    String superAdminSenha;

    @ConfigProperty(name = "bootstrap.super-admin.cpf", defaultValue = "00000000000")
    String superAdminCpf;

    @ConfigProperty(name = "bootstrap.super-admin.nome", defaultValue = "Super Admin")
    String superAdminNome;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    HashService hashService;

    @Transactional
    public void onStart(@Observes StartupEvent ev) {
        // Panache count() não aceita SELECT COUNT explícito; usamos MEMBER OF
        // sobre a coleção Perfil para perguntar "quantos Usuarios têm SUPER_ADMIN".
        long qtdSuper = usuarioRepository.count("?1 MEMBER OF perfis", Perfil.SUPER_ADMIN);
        if (qtdSuper > 0) {
            Log.debug("Bootstrap: já existe SUPER_ADMIN, nada a fazer.");
            return;
        }

        // Usa a Empresa Padrão (id=1) criada pela V3. Se por qualquer motivo
        // ela não existir (ambiente novo), cria uma empresa-sistema mínima.
        Empresa empresaSistema = empresaRepository.findById(1L);
        if (empresaSistema == null) {
            empresaSistema = new Empresa();
            empresaSistema.setNome("Empresa Padrão");
            empresaSistema.setCnpj("00000000000000");
            empresaSistema.setStatus(StatusEmpresa.ATIVA);
            empresaRepository.persist(empresaSistema);
            Log.info("Bootstrap: empresa-sistema criada (id=" + empresaSistema.getId() + ")");
        }

        Usuario admin = new Usuario();
        admin.setNome(superAdminNome);
        admin.setEmail(superAdminEmail);
        admin.setCpf(superAdminCpf);
        admin.setSenha(hashService.getHashSenha(superAdminSenha));
        admin.setEmpresa(empresaSistema);
        Set<Perfil> perfis = new HashSet<>();
        perfis.add(Perfil.SUPER_ADMIN);
        admin.setPerfis(perfis);
        usuarioRepository.persist(admin);
        Log.info("Bootstrap: SUPER_ADMIN criado com email=" + superAdminEmail);
    }
}
