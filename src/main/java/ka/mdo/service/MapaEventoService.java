package ka.mdo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import ka.mdo.dto.FormaMapaDTO;
import ka.mdo.dto.FormaMapaResponseDTO;
import ka.mdo.dto.MapaEventoDTO;
import ka.mdo.dto.MapaEventoResponseDTO;
import ka.mdo.model.Empresa;
import ka.mdo.model.EspacoEvento;
import ka.mdo.model.Evento;
import ka.mdo.model.FormaMapa;
import ka.mdo.model.MapaEvento;
import ka.mdo.model.TipoForma;
import ka.mdo.repository.EmpresaRepository;
import ka.mdo.repository.EspacoEventoRepository;
import ka.mdo.repository.EventoRepository;
import ka.mdo.repository.MapaEventoRepository;
import ka.mdo.storage.StorageService;
import ka.mdo.storage.StorageValidator;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Regras de negócio do mapa 2D do evento (atividade 040).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Upsert do mapa: PUT substitui o conjunto atual de formas — as antigas
 *       são removidas via {@code orphanRemoval}.</li>
 *   <li>Validação de tenant em duas frentes: o evento dono do mapa e cada
 *       {@link EspacoEvento} referenciado pelas formas precisam pertencer à
 *       empresa do JWT. Além disso, toda forma deve referenciar um espaço
 *       que está associado AO MESMO evento (sem empréstimo cross-evento).</li>
 *   <li>Validação de cor: regex estrita {@code ^#[0-9A-Fa-f]{6}$} (além do
 *       {@code @Pattern} no DTO, como defesa em profundidade).</li>
 *   <li>Validação da geometria: parse Jackson e checagem das chaves mínimas
 *       por {@link TipoForma}.</li>
 *   <li>Upload da imagem de fundo — bucket dedicado {@code mapas}
 *       (separado de {@code credenciais-foto}/{@code documentos} para não
 *       misturar domínios nem permissões futuras de IAM).</li>
 * </ul>
 */
@ApplicationScoped
public class MapaEventoService {

    private static final Logger LOG = Logger.getLogger(MapaEventoService.class);
    private static final Pattern COR_HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    MapaEventoRepository mapaRepository;

    @Inject
    EventoRepository eventoRepository;

    @Inject
    EspacoEventoRepository espacoRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    StorageService storageService;

    @Inject
    StorageValidator storageValidator;

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "storage.bucket.mapas", defaultValue = "mapas")
    String bucketMapas;

    // ---------- helpers ----------

    private Long empresaDoJwt() {
        Long empresaId = jwt.getClaim("empresaId");
        if (empresaId == null) {
            throw new ForbiddenException("JWT sem empresaId");
        }
        return empresaId;
    }

    private Evento carregarEventoDoTenant(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId);
        if (evento == null) {
            throw new NotFoundException("Evento não encontrado");
        }
        if (evento.getEmpresa() == null || !evento.getEmpresa().getId().equals(empresaDoJwt())) {
            throw new ForbiddenException("Evento pertence a outra empresa");
        }
        return evento;
    }

    // ---------- leitura ----------

    public MapaEventoResponseDTO buscarPorEvento(Long eventoId) {
        Evento evento = carregarEventoDoTenant(eventoId);
        MapaEvento mapa = mapaRepository.findByEventoId(evento.getId())
                .orElseThrow(() -> new NotFoundException("Mapa não cadastrado para este evento"));
        return toResponse(mapa);
    }

    // ---------- upsert ----------

    /**
     * Salva (upsert) o mapa de um evento. Se já existir mapa, as formas
     * antigas são descartadas (orphanRemoval) e as novas persistidas.
     */
    @Transactional
    public MapaEventoResponseDTO salvar(Long eventoId, MapaEventoDTO dto) {
        Evento evento = carregarEventoDoTenant(eventoId);

        MapaEvento mapa = mapaRepository.findByEventoId(evento.getId()).orElse(null);
        boolean novo = (mapa == null);
        if (novo) {
            mapa = new MapaEvento();
            Empresa empresa = empresaRepository.findById(empresaDoJwt());
            mapa.setEmpresa(empresa);
            mapa.setEvento(evento);
        } else {
            // Higieniza as formas antigas — orphanRemoval vai deletar as
            // que deixarmos fora da coleção.
            mapa.getFormas().clear();
        }

        mapa.setLargura(dto.largura());
        mapa.setAltura(dto.altura());
        mapa.setUnidade(dto.unidade() == null || dto.unidade().isBlank() ? "px" : dto.unidade().trim());

        // imagemFundoObjectKey: só permitimos passar via DTO se já existir
        // (por ex. clonagem). Nunca seta para string vazia — vira null.
        if (dto.imagemFundoObjectKey() != null && !dto.imagemFundoObjectKey().isBlank()) {
            mapa.setImagemFundoObjectKey(dto.imagemFundoObjectKey().trim());
        }

        // Valida e constrói formas novas.
        Long empresaId = empresaDoJwt();
        List<FormaMapa> formasNovas = new ArrayList<>();
        for (FormaMapaDTO formaDto : dto.formas()) {
            formasNovas.add(construirForma(mapa, evento, formaDto, empresaId));
        }
        mapa.getFormas().addAll(formasNovas);

        if (novo) {
            mapaRepository.persist(mapa);
        }
        // Em update, cascade + orphanRemoval tratam o resto no flush.

        LOG.infof("Mapa %s para evento id=%d com %d formas",
                novo ? "criado" : "atualizado", evento.getId(), formasNovas.size());
        return toResponse(mapa);
    }

    // ---------- upload de imagem de fundo ----------

    @Transactional
    public MapaEventoResponseDTO uploadImagemFundo(Long eventoId, byte[] bytes, String contentType) {
        storageValidator.validarImagem(bytes, contentType);
        Evento evento = carregarEventoDoTenant(eventoId);
        MapaEvento mapa = mapaRepository.findByEventoId(evento.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Mapa não cadastrado para este evento — faça PUT /mapa antes do upload"));

        String ext = extensaoPorContentType(contentType);
        String key = String.format("empresa-%d/evento-%d/fundo.%s",
                mapa.getEmpresa().getId(), evento.getId(), ext);
        storageService.upload(bucketMapas, key, bytes, contentType);
        mapa.setImagemFundoObjectKey(key);
        LOG.infof("Imagem de fundo do mapa evento=%d salva em bucket=%s key=%s",
                evento.getId(), bucketMapas, key);
        return toResponse(mapa);
    }

    // ---------- validação de forma ----------

    private FormaMapa construirForma(MapaEvento mapa, Evento evento, FormaMapaDTO dto, Long empresaId) {
        // Cor: defesa em profundidade mesmo com @Pattern no DTO.
        if (dto.corHex() == null || !COR_HEX.matcher(dto.corHex()).matches()) {
            throw new BadRequestException("corHex inválido: esperado formato #RRGGBB");
        }
        if (dto.tipo() == null) {
            throw new BadRequestException("tipo da forma é obrigatório");
        }
        if (dto.espacoId() == null) {
            throw new BadRequestException("espacoId obrigatório em todas as formas");
        }

        EspacoEvento espaco = espacoRepository.findById(dto.espacoId());
        if (espaco == null) {
            throw new NotFoundException("EspacoEvento id=" + dto.espacoId() + " não encontrado");
        }
        if (espaco.getEmpresa() == null || !espaco.getEmpresa().getId().equals(empresaId)) {
            throw new ForbiddenException("EspacoEvento id=" + dto.espacoId() + " pertence a outra empresa");
        }
        if (!espacoPertenceAoEvento(evento, espaco)) {
            throw new BadRequestException(
                    "EspacoEvento id=" + espaco.getId() + " não pertence ao evento id=" + evento.getId());
        }

        validarGeometria(dto.tipo(), dto.geometria());

        FormaMapa forma = new FormaMapa();
        forma.setMapa(mapa);
        forma.setEspaco(espaco);
        forma.setTipo(dto.tipo());
        forma.setCorHex(dto.corHex());
        forma.setRotulo(dto.rotulo());
        try {
            forma.setGeometriaJson(MAPPER.writeValueAsString(dto.geometria()));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Falha ao serializar geometria: " + e.getOriginalMessage());
        }
        return forma;
    }

    private boolean espacoPertenceAoEvento(Evento evento, EspacoEvento espaco) {
        if (evento.getEspacoEventos() == null) {
            return false;
        }
        return evento.getEspacoEventos().stream()
                .anyMatch(e -> e.getId() != null && e.getId().equals(espaco.getId()));
    }

    /**
     * Valida que o payload {@code geometria} tem as chaves mínimas esperadas
     * para o {@code tipo}. Não checa limites (negativos, fora da área do
     * mapa) — isso é responsabilidade do frontend; o backend só garante que
     * a estrutura é consumível.
     */
    void validarGeometria(TipoForma tipo, Map<String, Object> geometria) {
        if (geometria == null || geometria.isEmpty()) {
            throw new BadRequestException("geometria obrigatória");
        }
        switch (tipo) {
            case RETANGULO -> {
                exigirNumero(geometria, "x");
                exigirNumero(geometria, "y");
                exigirNumero(geometria, "w");
                exigirNumero(geometria, "h");
            }
            case CIRCULO -> {
                exigirNumero(geometria, "cx");
                exigirNumero(geometria, "cy");
                exigirNumero(geometria, "r");
            }
            case POLIGONO -> {
                Object pontosRaw = geometria.get("pontos");
                if (!(pontosRaw instanceof List<?> pontos) || pontos.size() < 3) {
                    throw new BadRequestException(
                            "POLIGONO requer 'pontos' como lista de pelo menos 3 pares [x,y]");
                }
                for (Object p : pontos) {
                    if (!(p instanceof List<?> par) || par.size() != 2) {
                        throw new BadRequestException(
                                "Cada ponto do POLIGONO deve ser um par [x,y] numérico");
                    }
                    if (!(par.get(0) instanceof Number) || !(par.get(1) instanceof Number)) {
                        throw new BadRequestException(
                                "Coordenadas do POLIGONO devem ser numéricas");
                    }
                }
            }
        }
    }

    private void exigirNumero(Map<String, Object> geometria, String chave) {
        Object v = geometria.get(chave);
        if (!(v instanceof Number)) {
            throw new BadRequestException(
                    "geometria." + chave + " obrigatório e numérico");
        }
    }

    private String extensaoPorContentType(String contentType) {
        if (contentType == null) return "bin";
        String ct = contentType.toLowerCase();
        if (ct.contains("jpeg") || ct.contains("jpg")) return "jpg";
        if (ct.contains("png")) return "png";
        if (ct.contains("webp")) return "webp";
        return "bin";
    }

    // ---------- converter ----------

    @SuppressWarnings("unchecked")
    private MapaEventoResponseDTO toResponse(MapaEvento mapa) {
        String imagemUrl = mapa.getImagemFundoObjectKey() == null ? null
                : storageService.downloadUrl(bucketMapas, mapa.getImagemFundoObjectKey());

        List<FormaMapaResponseDTO> formasDto = new ArrayList<>();
        for (FormaMapa f : mapa.getFormas()) {
            Map<String, Object> geometria;
            try {
                geometria = MAPPER.readValue(f.getGeometriaJson(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                // Defensivo: se o JSON estiver corrompido no banco, não
                // quebra o GET — devolve mapa vazio para essa forma.
                LOG.warnf(e, "Geometria corrompida na forma id=%d", f.getId());
                geometria = Map.of();
            }
            formasDto.add(new FormaMapaResponseDTO(
                    f.getId(),
                    f.getEspaco().getId(),
                    f.getEspaco().getNome(),
                    f.getTipo(),
                    geometria,
                    f.getCorHex(),
                    f.getRotulo()));
        }

        return new MapaEventoResponseDTO(
                mapa.getId(),
                mapa.getEvento().getId(),
                mapa.getLargura(),
                mapa.getAltura(),
                mapa.getUnidade(),
                imagemUrl,
                formasDto);
    }
}
