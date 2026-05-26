package ka.mdo.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import ka.mdo.dto.AparelhoDTO;
import ka.mdo.dto.AparelhoResponseDTO;
import ka.mdo.model.Aparelho;
import ka.mdo.model.Empresa;
import ka.mdo.model.EspacoEvento;
import ka.mdo.model.Evento;
import ka.mdo.repository.AparelhoRepository;
import ka.mdo.repository.EmpresaRepository;
import ka.mdo.repository.EspacoEventoRepository;
import ka.mdo.repository.EventoRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Regras de negócio do CRUD de {@link Aparelho} (atividade 014).
 *
 * <p>O isolamento por tenant vem do {@code tenantFilter} do Hibernate
 * (configurado em {@link Aparelho}). Todas as buscas via repositório já
 * retornam apenas registros da empresa do JWT — uma referência {@code null}
 * em {@code findById} significa "não existe ou pertence a outro tenant".
 *
 * <p>Validação cross-tenant para {@code eventoId}/{@code localEspecificoId}:
 * como {@link Evento} e {@link EspacoEvento} também usam o {@code tenantFilter},
 * basta confiar no {@code findById} filtrado — uma referência a recurso de
 * outro tenant volta {@code null} e nós respondemos 403.
 */
@ApplicationScoped
public class AparelhoService {

    @Inject
    AparelhoRepository repository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    EventoRepository eventoRepository;

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

    public List<AparelhoResponseDTO> listar(Boolean ativo,
                                            Long eventoId,
                                            Long localEspecificoId,
                                            int pagina,
                                            int tamanho) {
        // empresaDoJwt() valida 403 cedo se o token vier sem o claim; o filtro
        // Hibernate cuida do resto.
        empresaDoJwt();
        return repository.listarFiltrado(ativo, eventoId, localEspecificoId, pagina, tamanho)
                .stream()
                .map(AparelhoResponseDTO::new)
                .collect(Collectors.toList());
    }

    public AparelhoResponseDTO buscarPorId(Long id) {
        empresaDoJwt();
        Aparelho a = repository.findById(id);
        if (a == null) {
            // Pode ser inexistente ou de outro tenant — em ambos os casos o
            // contrato é o mesmo: 404.
            throw new NotFoundException("Aparelho não encontrado");
        }
        return new AparelhoResponseDTO(a);
    }

    @Transactional
    public AparelhoResponseDTO criar(AparelhoDTO dto) {
        Long empresaId = empresaDoJwt();
        Empresa empresa = empresaRepository.findById(empresaId);
        if (empresa == null) {
            throw new ForbiddenException("Empresa do JWT inexistente");
        }

        Aparelho a = new Aparelho();
        a.setDescricao(dto.descricao());
        a.setEmpresa(empresa);
        a.setEvento(resolverEventoOuFalhar(dto.eventoId()));
        a.setLocalEspecifico(resolverLocalOuFalhar(dto.localEspecificoId()));
        repository.persist(a);
        Log.infof("Aparelho %d criado (empresa=%d)", a.getId(), empresaId);
        return new AparelhoResponseDTO(a);
    }

    @Transactional
    public AparelhoResponseDTO atualizar(Long id, AparelhoDTO dto) {
        empresaDoJwt();
        Aparelho a = repository.findById(id);
        if (a == null) {
            throw new NotFoundException("Aparelho não encontrado");
        }
        a.setDescricao(dto.descricao());
        a.setEvento(resolverEventoOuFalhar(dto.eventoId()));
        a.setLocalEspecifico(resolverLocalOuFalhar(dto.localEspecificoId()));
        return new AparelhoResponseDTO(a);
    }

    @Transactional
    public AparelhoResponseDTO desativar(Long id) {
        return alternarAtivo(id, false);
    }

    @Transactional
    public AparelhoResponseDTO reativar(Long id) {
        return alternarAtivo(id, true);
    }

    private AparelhoResponseDTO alternarAtivo(Long id, boolean novoEstado) {
        empresaDoJwt();
        Aparelho a = repository.findById(id);
        if (a == null) {
            throw new NotFoundException("Aparelho não encontrado");
        }
        // EntityClass.prePersist força ativo=true só na criação; aqui é UPDATE
        // explícito e o flush da transação cuida da persistência.
        a.setAtivo(novoEstado);
        return new AparelhoResponseDTO(a);
    }

    /**
     * Resolve um {@link Evento} pelo id (ou null) garantindo que pertence ao
     * tenant do JWT. Como {@code Evento} usa {@code tenantFilter}, um id de
     * outro tenant volta {@code null} aqui — respondemos 403 explícito para
     * que o painel não confunda com "evento inexistente do meu tenant".
     */
    private Evento resolverEventoOuFalhar(Long eventoId) {
        if (eventoId == null) {
            return null;
        }
        Evento e = eventoRepository.findById(eventoId);
        if (e == null) {
            throw new ForbiddenException("Evento pertence a outra empresa ou não existe");
        }
        return e;
    }

    private EspacoEvento resolverLocalOuFalhar(Long localEspecificoId) {
        if (localEspecificoId == null) {
            return null;
        }
        EspacoEvento ee = espacoEventoRepository.findById(localEspecificoId);
        if (ee == null) {
            throw new ForbiddenException("Espaço pertence a outra empresa ou não existe");
        }
        return ee;
    }
}
