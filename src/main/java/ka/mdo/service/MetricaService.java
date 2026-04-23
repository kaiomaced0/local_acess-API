package ka.mdo.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import ka.mdo.dto.EntradaHoraDTO;
import ka.mdo.dto.OcupacaoLocalDTO;
import ka.mdo.dto.PendenciaLocalDTO;
import ka.mdo.dto.PendenciaStatusDTO;
import ka.mdo.model.Evento;
import ka.mdo.model.StatusPendencia;
import ka.mdo.repository.EventoRepository;
import ka.mdo.repository.GestorLocalRepository;
import ka.mdo.repository.LogAcessoRepository;
import ka.mdo.repository.PendenciaRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Serviço das métricas agregadas usadas pelo dashboard do gestor (atividade 041).
 *
 * <h3>Cache</h3>
 * Todas as leituras passam por um cache simples em memória com TTL de 30s.
 * O cache é invalidado exclusivamente pelo TTL — mutações (novas leituras /
 * pendências) não sinalizam invalidação explícita, já que o dashboard aceita
 * atraso de até 30s por definição do enunciado. Implementamos um mapa
 * {@link ConcurrentHashMap} sob um {@link AtomicReference} para evitar a
 * dependência opcional {@code quarkus-cache} — a complexidade não se paga
 * aqui.
 *
 * <h3>Multitenancy</h3>
 * O filtro {@code tenantFilter} do Hibernate isola por empresa automaticamente
 * nas queries. Adicionalmente validamos que o {@code eventoId} recebido
 * pertence à empresa do JWT (defesa em profundidade).
 *
 * <h3>Gestor de local</h3>
 * Quando o chamador é {@link ka.mdo.model.Perfil#GESTOR_LOCAL} (sem perfil
 * "acima"), as queries são restringidas aos locais vinculados via
 * {@link ka.mdo.model.GestorLocal} — fechando o débito da 030/031.
 */
@ApplicationScoped
public class MetricaService {

    private static final Logger LOG = Logger.getLogger(MetricaService.class);

    /** Vida do cache de métricas (enunciado da 041). */
    static final Duration CACHE_TTL = Duration.ofSeconds(30);

    /** Range default para entradas quando o caller não informa. */
    static final Duration DEFAULT_RANGE_ENTRADAS = Duration.ofHours(24);

    /** Range máximo permitido para entradas. */
    static final Duration MAX_RANGE_ENTRADAS = Duration.ofDays(7);

    @Inject
    LogAcessoRepository logAcessoRepository;

    @Inject
    PendenciaRepository pendenciaRepository;

    @Inject
    EventoRepository eventoRepository;

    @Inject
    GestorLocalRepository gestorLocalRepository;

    @Inject
    JsonWebToken jwt;

    /**
     * Mapa chave → entrada. A chave inclui o eventoId e um tag por tipo de
     * métrica/range para evitar colisão entre respostas diferentes.
     * {@link AtomicReference} apenas para permitir um eventual "clear" atômico
     * (teste, por exemplo); as leituras cotidianas vão no {@code ConcurrentHashMap}.
     */
    private final AtomicReference<ConcurrentHashMap<CacheKey, CacheEntry>> cacheRef =
            new AtomicReference<>(new ConcurrentHashMap<>());

    /**
     * Pessoas atualmente dentro de cada local do evento.
     */
    public List<OcupacaoLocalDTO> ocupacao(Long eventoId) {
        validarAcessoAoEvento(eventoId);
        Collection<Long> locais = locaisDoGestorLocalOuNull();
        CacheKey key = new CacheKey(eventoId, "ocupacao", null, locaisKeyPart(locais));
        Object cached = get(key);
        if (cached != null) {
            @SuppressWarnings("unchecked")
            List<OcupacaoLocalDTO> hit = (List<OcupacaoLocalDTO>) cached;
            return hit;
        }

        List<Object[]> rows = logAcessoRepository.ocupacaoPorLocal(eventoId, locais);
        List<OcupacaoLocalDTO> resp = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long localId = (Long) r[0];
            String nome = (String) r[1];
            long ocup = r[2] == null ? 0L : ((Number) r[2]).longValue();
            if (ocup < 0) {
                // Mais saídas que entradas — tratamos como 0 (inconsistência
                // esperada quando aparelhos de saída forem instalados depois de
                // o evento começar; não queremos expor negativos no dashboard).
                ocup = 0;
            }
            resp.add(new OcupacaoLocalDTO(localId, nome, ocup));
        }
        put(key, resp);
        return resp;
    }

    /**
     * Entradas agregadas por hora entre {@code de} e {@code ate}.
     *
     * <p>{@code granularidade} aceita apenas {@code "hora"} hoje (o enunciado
     * explicita). Range default: últimas 24h. Máximo: 7 dias.
     */
    public List<EntradaHoraDTO> entradas(Long eventoId,
                                         String granularidade,
                                         LocalDateTime de,
                                         LocalDateTime ate) {
        validarAcessoAoEvento(eventoId);

        if (granularidade != null && !granularidade.isBlank()
                && !"hora".equalsIgnoreCase(granularidade)) {
            throw new BadRequestException("granularidade não suportada: " + granularidade
                    + " (apenas 'hora' por enquanto)");
        }

        LocalDateTime ateResolvido = ate != null ? ate : LocalDateTime.now();
        LocalDateTime deResolvido = de != null
                ? de
                : ateResolvido.minus(DEFAULT_RANGE_ENTRADAS);
        if (!deResolvido.isBefore(ateResolvido)) {
            throw new BadRequestException("de deve ser anterior a ate");
        }
        if (Duration.between(deResolvido, ateResolvido).compareTo(MAX_RANGE_ENTRADAS) > 0) {
            throw new BadRequestException("intervalo máximo permitido é 7 dias");
        }

        Collection<Long> locais = locaisDoGestorLocalOuNull();
        CacheKey key = new CacheKey(eventoId, "entradas",
                deResolvido + "|" + ateResolvido, locaisKeyPart(locais));
        Object cached = get(key);
        if (cached != null) {
            @SuppressWarnings("unchecked")
            List<EntradaHoraDTO> hit = (List<EntradaHoraDTO>) cached;
            return hit;
        }

        // Agregação em Java: decisão documentada no LogAcessoRepository.
        List<LocalDateTime> entradas = logAcessoRepository
                .entradasAutorizadasNoPeriodo(eventoId, deResolvido, ateResolvido, locais);

        // Inicializa todos os buckets com 0 para respostas densas (cliente não
        // precisa lidar com horas ausentes).
        Map<LocalDateTime, Long> buckets = new TreeMap<>();
        LocalDateTime cursor = deResolvido.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime fim = ateResolvido.truncatedTo(ChronoUnit.HOURS);
        while (!cursor.isAfter(fim)) {
            buckets.put(cursor, 0L);
            cursor = cursor.plusHours(1);
        }
        for (LocalDateTime dh : entradas) {
            LocalDateTime bucket = dh.truncatedTo(ChronoUnit.HOURS);
            buckets.merge(bucket, 1L, Long::sum);
        }

        List<EntradaHoraDTO> resp = new ArrayList<>(buckets.size());
        for (Map.Entry<LocalDateTime, Long> e : buckets.entrySet()) {
            resp.add(new EntradaHoraDTO(e.getKey(), e.getValue()));
        }
        put(key, resp);
        return resp;
    }

    /**
     * Contagem de pendências agrupada por {@link StatusPendencia} e por local.
     *
     * <p>Retorna um mapa com duas chaves: {@code "status"} (lista
     * {@link PendenciaStatusDTO}) e {@code "porLocal"} (lista
     * {@link PendenciaLocalDTO}). Um único endpoint costura as duas visões
     * que o dashboard exibe lado a lado, economizando round-trips.
     */
    public PendenciaAgregadaResponse pendencias(Long eventoId) {
        validarAcessoAoEvento(eventoId);

        Collection<Long> locais = locaisDoGestorLocalOuNull();
        CacheKey key = new CacheKey(eventoId, "pendencias", null, locaisKeyPart(locais));
        Object cached = get(key);
        if (cached != null) {
            return (PendenciaAgregadaResponse) cached;
        }

        List<Object[]> statusRows = pendenciaRepository.totalPorStatus(locais);
        List<PendenciaStatusDTO> porStatus = new ArrayList<>(statusRows.size());
        for (Object[] r : statusRows) {
            porStatus.add(new PendenciaStatusDTO(
                    (StatusPendencia) r[0],
                    r[1] == null ? 0L : ((Number) r[1]).longValue()));
        }

        List<Object[]> localRows = pendenciaRepository.abertasPorLocal(locais);
        List<PendenciaLocalDTO> porLocal = new ArrayList<>(localRows.size());
        for (Object[] r : localRows) {
            porLocal.add(new PendenciaLocalDTO(
                    (Long) r[0],
                    (String) r[1],
                    r[2] == null ? 0L : ((Number) r[2]).longValue()));
        }

        PendenciaAgregadaResponse resp = new PendenciaAgregadaResponse(
                Collections.unmodifiableList(porStatus),
                Collections.unmodifiableList(porLocal));
        put(key, resp);
        return resp;
    }

    // -------- helpers --------

    private void validarAcessoAoEvento(Long eventoId) {
        if (eventoId == null) {
            throw new BadRequestException("eventoId obrigatório");
        }
        Long empresaJwt = jwt.getClaim("empresaId");
        if (empresaJwt == null && !isSuperAdmin()) {
            throw new ForbiddenException("JWT sem empresaId");
        }
        Evento evento = eventoRepository.findById(eventoId);
        if (evento == null) {
            throw new NotFoundException("Evento não encontrado: id=" + eventoId);
        }
        // Tenant-check explícito (defesa em profundidade).
        if (empresaJwt != null
                && evento.getEmpresa() != null
                && !Objects.equals(evento.getEmpresa().getId(), empresaJwt)) {
            throw new ForbiddenException("Evento pertence a outra empresa");
        }
    }

    private boolean isSuperAdmin() {
        var groups = jwt.getGroups();
        return groups != null && groups.contains("SUPER_ADMIN");
    }

    /**
     * Se o chamador é {@code GESTOR_LOCAL} puro (sem perfil acima), devolve
     * a lista de ids de {@link ka.mdo.model.EspacoEvento} vinculados; senão
     * {@code null} (sem restrição adicional).
     */
    private Collection<Long> locaisDoGestorLocalOuNull() {
        var groups = jwt.getGroups();
        if (groups == null) {
            return null;
        }
        boolean ehGestorLocal = groups.contains("GESTOR_LOCAL");
        boolean temPerfilAcima = groups.contains("SUPER_ADMIN")
                || groups.contains("ADMIN_EMPRESA")
                || groups.contains("GESTOR_EVENTO");
        if (!ehGestorLocal || temPerfilAcima) {
            return null;
        }
        Long usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) {
            return List.of();
        }
        return gestorLocalRepository.findLocaisDoGestor(usuarioId);
    }

    private static String locaisKeyPart(Collection<Long> locais) {
        if (locais == null) return "*";
        if (locais.isEmpty()) return "[]";
        // Ordena para estabilidade da chave.
        List<Long> ordered = new ArrayList<>(locais);
        Collections.sort(ordered);
        return ordered.toString();
    }

    private Object get(CacheKey key) {
        CacheEntry e = cacheRef.get().get(key);
        if (e == null) {
            return null;
        }
        if (e.expiresAt.isBefore(LocalDateTime.now())) {
            cacheRef.get().remove(key, e);
            return null;
        }
        return e.value;
    }

    private void put(CacheKey key, Object value) {
        cacheRef.get().put(key, new CacheEntry(value, LocalDateTime.now().plus(CACHE_TTL)));
    }

    /** Reset do cache — usado por testes. */
    void limparCache() {
        cacheRef.set(new ConcurrentHashMap<>());
    }

    private record CacheKey(Long eventoId, String tipo, String bucket, String locaisKey) {}

    private record CacheEntry(Object value, LocalDateTime expiresAt) {}

    /**
     * Agregado devolvido por {@link #pendencias(Long)}.
     */
    public record PendenciaAgregadaResponse(
            List<PendenciaStatusDTO> status,
            List<PendenciaLocalDTO> porLocal) {
    }
}
