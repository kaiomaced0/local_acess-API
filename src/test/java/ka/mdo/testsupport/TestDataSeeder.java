package ka.mdo.testsupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ka.mdo.model.Aparelho;
import ka.mdo.model.Empresa;
import ka.mdo.model.Evento;
import ka.mdo.model.Ingresso;
import ka.mdo.model.Pendencia;
import ka.mdo.model.Perfil;
import ka.mdo.model.StatusEmpresa;
import ka.mdo.model.StatusPendencia;
import ka.mdo.model.TipoIngresso;
import ka.mdo.model.Usuario;
import ka.mdo.repository.AparelhoRepository;
import ka.mdo.repository.EmpresaRepository;
import ka.mdo.repository.EventoRepository;
import ka.mdo.repository.IngressoRepository;
import ka.mdo.repository.PendenciaRepository;
import ka.mdo.repository.TipoIngressoRepository;
import ka.mdo.repository.UsuarioRepository;
import ka.mdo.service.HashService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Semeia dados para os testes de integração (atividade 051).
 *
 * <p>Todos os métodos são {@code @Transactional} e invocados como chamadas
 * diretas ao bean (fora de uma requisição HTTP). Por isso o {@code tenantFilter}
 * do Hibernate <b>não</b> está ativo — é o que permite criar dados de empresas
 * diferentes na mesma transação sem o isolamento atrapalhar o setup.
 */
@ApplicationScoped
public class TestDataSeeder {

    /** CPF sintaticamente válido (dígitos verificadores corretos). Sem unique no schema. */
    public static final String CPF_VALIDO = "52998224725";

    @Inject EmpresaRepository empresaRepository;
    @Inject AparelhoRepository aparelhoRepository;
    @Inject IngressoRepository ingressoRepository;
    @Inject EventoRepository eventoRepository;
    @Inject TipoIngressoRepository tipoIngressoRepository;
    @Inject UsuarioRepository usuarioRepository;
    @Inject PendenciaRepository pendenciaRepository;
    @Inject HashService hashService;

    @Transactional
    public Long criarEmpresa(String nome) {
        Empresa e = new Empresa();
        e.setNome(nome);
        e.setCnpj(digitos(14));
        e.setStatus(StatusEmpresa.ATIVA);
        empresaRepository.persist(e);
        return e.getId();
    }

    /** Aparelho ativo do tenant. {@code eventoId} nulo = entrada geral (sem evento). */
    @Transactional
    public Long criarAparelho(Long empresaId, Long eventoId) {
        Aparelho a = new Aparelho();
        a.setDescricao("aparelho-teste");
        a.setEmpresa(empresaRepository.findById(empresaId));
        if (eventoId != null) {
            a.setEvento(eventoRepository.findById(eventoId));
        }
        aparelhoRepository.persist(a);
        return a.getId();
    }

    /** Cria uma credencial comum no tenant e devolve o token opaco (payload do QR). */
    @Transactional
    public String criarIngresso(Long empresaId) {
        Ingresso i = new Ingresso();
        i.setEmpresa(empresaRepository.findById(empresaId));
        i.setToken(novoToken());
        ingressoRepository.persist(i);
        return i.getToken();
    }

    /** Evento que exige validação facial — base do caminho PENDENTE FOTO_FACIAL_AUSENTE. */
    @Transactional
    public Long criarEventoFacial(Long empresaId) {
        Evento ev = new Evento();
        ev.setNome("evento-facial");
        ev.setEmpresa(empresaRepository.findById(empresaId));
        ev.setValidarFacial(true);
        eventoRepository.persist(ev);
        return ev.getId();
    }

    @Transactional
    public Long criarUsuarioCliente(Long empresaId) {
        Usuario u = new Usuario();
        u.setNome("Cliente Teste");
        u.setEmail("cli-" + UUID.randomUUID() + "@teste.local");
        u.setCpf(CPF_VALIDO);
        u.setSenha(hashService.getHashSenha("senha123"));
        u.setEmpresa(empresaRepository.findById(empresaId));
        u.setPerfis(new HashSet<>(Set.of(Perfil.CLIENTE)));
        u.setIngressos(new ArrayList<>());
        usuarioRepository.persist(u);
        return u.getId();
    }

    @Transactional
    public Long criarTipoIngresso(Long empresaId) {
        TipoIngresso t = new TipoIngresso();
        t.setNome("Pista");
        t.setEmpresa(empresaRepository.findById(empresaId));
        tipoIngressoRepository.persist(t);
        return t.getId();
    }

    /**
     * Cria uma pendência ABERTA (motivo não-facial, sem foto) pronta para o teste
     * de aprovação. Cria a credencial e o aparelho de suporte no mesmo tenant.
     */
    @Transactional
    public Long criarPendenciaAberta(Long empresaId) {
        Empresa e = empresaRepository.findById(empresaId);

        Ingresso credencial = new Ingresso();
        credencial.setEmpresa(e);
        credencial.setToken(novoToken());
        ingressoRepository.persist(credencial);

        Aparelho aparelho = new Aparelho();
        aparelho.setDescricao("aparelho-pendencia");
        aparelho.setEmpresa(e);
        aparelhoRepository.persist(aparelho);

        Pendencia p = new Pendencia();
        p.setEmpresa(e);
        p.setCredencial(credencial);
        p.setAparelho(aparelho);
        p.setMotivo("DADOS_PESSOAIS_INCOMPLETOS");
        p.setStatus(StatusPendencia.ABERTA);
        p.setCriadaEm(LocalDateTime.now());
        pendenciaRepository.persist(p);
        return p.getId();
    }

    /** Leitura do token persistido — usada para confirmar a emissão de credencial. */
    @Transactional
    public String tokenDoIngresso(Long ingressoId) {
        Ingresso i = ingressoRepository.findById(ingressoId);
        return i == null ? null : i.getToken();
    }

    private static String novoToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String digitos(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }
}
