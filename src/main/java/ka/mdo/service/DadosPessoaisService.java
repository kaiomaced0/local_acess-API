package ka.mdo.service;

import java.util.Optional;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import ka.mdo.dto.DadosPessoaisDTO;
import ka.mdo.dto.DadosPessoaisResponseDTO;
import ka.mdo.model.DadosPessoais;
import ka.mdo.model.Empresa;
import ka.mdo.model.Perfil;
import ka.mdo.model.TipoDocumento;
import ka.mdo.model.Usuario;
import ka.mdo.repository.DadosPessoaisRepository;
import ka.mdo.repository.EmpresaRepository;
import ka.mdo.repository.UsuarioRepository;
import ka.mdo.storage.StorageService;
import ka.mdo.storage.StorageValidator;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Regras de negócio para {@link DadosPessoais}.
 *
 * <p>Responsabilidades:
 * <ul>
 *     <li>Criar/atualizar dados do usuário logado a partir de {@link DadosPessoaisDTO}
 *     (com normalização e validação do documento).</li>
 *     <li>Fazer upload da foto (selfie) e da foto do documento nos buckets corretos
 *     via {@link StorageService}, depois de {@link StorageValidator#validarImagem(byte[], String)}.</li>
 *     <li>Gerar URLs pré-assinadas para leitura (TTL curto do próprio storage).</li>
 *     <li>Manter o isolamento por tenant — toda operação checa que
 *     {@code dados.empresa.id == jwt.empresaId} e responde 403 caso contrário.</li>
 * </ul>
 *
 * <p><b>LGPD</b>: nunca loga {@code nomeCompleto} nem {@code documento} em claro.
 */
@ApplicationScoped
public class DadosPessoaisService {

    private static final Logger LOG = Logger.getLogger(DadosPessoaisService.class);

    @Inject
    DadosPessoaisRepository repository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    StorageService storageService;

    @Inject
    StorageValidator storageValidator;

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "storage.bucket.credenciais-foto", defaultValue = "credenciais-foto")
    String bucketCredenciaisFoto;

    @ConfigProperty(name = "storage.bucket.documentos", defaultValue = "documentos")
    String bucketDocumentos;

    // ---------- tenant / auth helpers ----------

    private Long empresaDoJwt() {
        Long empresaId = jwt.getClaim("empresaId");
        if (empresaId == null) {
            throw new ForbiddenException("JWT sem empresaId");
        }
        return empresaId;
    }

    private Long usuarioDoJwt() {
        Long usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) {
            // fallback: algumas implementações emitem `sub` com o id.
            String sub = jwt.getSubject();
            try {
                return sub == null ? null : Long.parseLong(sub);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return usuarioId;
    }

    private void validarMesmoTenant(DadosPessoais dp) {
        if (dp == null || dp.getEmpresa() == null
                || !dp.getEmpresa().getId().equals(empresaDoJwt())) {
            throw new ForbiddenException("Recurso pertence a outra empresa");
        }
    }

    private boolean ehGestor() {
        return jwt.getGroups().stream().anyMatch(g ->
                g.equals(Perfil.SUPER_ADMIN.name())
                        || g.equals(Perfil.ADMIN_EMPRESA.name())
                        || g.equals(Perfil.GESTOR_EVENTO.name())
                        || g.equals(Perfil.GESTOR_LOCAL.name()));
    }

    /**
     * Autoriza operação sobre {@code dp}: o próprio dono, ou um gestor do mesmo
     * tenant. Qualquer outra combinação → 403.
     */
    private void autorizarDonoOuGestor(DadosPessoais dp) {
        validarMesmoTenant(dp);
        Long usuarioJwt = usuarioDoJwt();
        boolean donoDosDados = usuarioJwt != null
                && dp.getUsuario() != null
                && usuarioJwt.equals(dp.getUsuario().getId());
        if (!donoDosDados && !ehGestor()) {
            throw new ForbiddenException("Apenas o dono dos dados ou um gestor pode acessar este recurso");
        }
    }

    // ---------- documento ----------

    /**
     * Normaliza o documento conforme o tipo. Para CPF, remove máscara e deixa
     * apenas dígitos. Para demais tipos, apenas faz {@code trim}.
     */
    String normalizarDocumento(TipoDocumento tipo, String documento) {
        if (documento == null) {
            return null;
        }
        if (tipo == TipoDocumento.CPF) {
            return documento.replaceAll("\\D", "");
        }
        return documento.trim();
    }

    /**
     * Valida CPF pelos dígitos verificadores (algoritmo oficial da Receita).
     * Implementado inline para evitar dependência nova (caelum-stella).
     */
    static boolean isCpfValido(String cpf) {
        if (cpf == null || cpf.length() != 11 || !cpf.matches("\\d{11}")) {
            return false;
        }
        // CPFs com todos dígitos iguais (ex: "00000000000") são inválidos mas
        // passam no cálculo — rejeita explicitamente.
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += (cpf.charAt(i) - '0') * (10 - i);
        }
        int d1 = 11 - (soma % 11);
        if (d1 >= 10) d1 = 0;
        if (d1 != (cpf.charAt(9) - '0')) return false;

        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += (cpf.charAt(i) - '0') * (11 - i);
        }
        int d2 = 11 - (soma % 11);
        if (d2 >= 10) d2 = 0;
        return d2 == (cpf.charAt(10) - '0');
    }

    /**
     * Mascara o documento para exibição. Para CPF (11 dígitos) retorna
     * {@code ***.***.***-XX}; demais tipos retornam os últimos 2 chars visíveis.
     */
    static String mascarar(TipoDocumento tipo, String documento) {
        if (documento == null || documento.isEmpty()) {
            return null;
        }
        if (tipo == TipoDocumento.CPF && documento.length() == 11) {
            return "***.***.***-" + documento.substring(9);
        }
        if (documento.length() <= 2) {
            return "**";
        }
        return "*".repeat(documento.length() - 2) + documento.substring(documento.length() - 2);
    }

    // ---------- CRUD ----------

    /**
     * Cria ou atualiza os dados pessoais do usuário logado (upsert).
     */
    @Transactional
    public DadosPessoaisResponseDTO salvarParaUsuarioLogado(DadosPessoaisDTO dto) {
        Long usuarioId = usuarioDoJwt();
        if (usuarioId == null) {
            throw new ForbiddenException("JWT sem identificação de usuário");
        }
        Long empresaId = empresaDoJwt();
        Usuario usuario = usuarioRepository.findById(usuarioId);
        if (usuario == null) {
            throw new NotFoundException("Usuário do JWT não encontrado");
        }
        if (usuario.getEmpresa() == null || !usuario.getEmpresa().getId().equals(empresaId)) {
            throw new ForbiddenException("Usuário de outro tenant");
        }

        String documentoNormalizado = normalizarDocumento(dto.tipoDocumento(), dto.documento());
        if (dto.tipoDocumento() == TipoDocumento.CPF && !isCpfValido(documentoNormalizado)) {
            throw new BadRequestException("CPF inválido");
        }

        DadosPessoais dp = usuario.getDadosPessoais();
        boolean novo = (dp == null);
        if (novo) {
            dp = new DadosPessoais();
            Empresa empresa = empresaRepository.findById(empresaId);
            dp.setEmpresa(empresa);
        }

        // Checagem de unicidade (empresa_id, tipo, documento): só se mudou.
        if (novo
                || dp.getTipoDocumento() != dto.tipoDocumento()
                || !documentoNormalizado.equals(dp.getDocumento())) {
            Optional<DadosPessoais> existente = repository.findByDocumento(dto.tipoDocumento(), documentoNormalizado);
            if (existente.isPresent() && (novo || !existente.get().getId().equals(dp.getId()))) {
                throw new BadRequestException("Já existe um cadastro com este documento para o tipo informado");
            }
        }

        dp.setNomeCompleto(dto.nomeCompleto().trim());
        dp.setTipoDocumento(dto.tipoDocumento());
        dp.setDocumento(documentoNormalizado);
        dp.setDataNascimento(dto.dataNascimento());

        if (novo) {
            repository.persist(dp);
            usuario.setDadosPessoais(dp);
        }

        LOG.infof("Dados pessoais %s (id=%d) para usuário %d", novo ? "criados" : "atualizados",
                dp.getId(), usuarioId);
        return toResponse(dp);
    }

    public DadosPessoaisResponseDTO buscarDoUsuarioLogado() {
        Long usuarioId = usuarioDoJwt();
        if (usuarioId == null) {
            throw new ForbiddenException("JWT sem identificação de usuário");
        }
        Optional<DadosPessoais> opt = repository.findByUsuarioId(usuarioId);
        if (opt.isEmpty()) {
            throw new NotFoundException("Dados pessoais ainda não preenchidos");
        }
        DadosPessoais dp = opt.get();
        validarMesmoTenant(dp);
        return toResponse(dp);
    }

    public DadosPessoaisResponseDTO buscarPorId(Long id, boolean incluirDocumento) {
        DadosPessoais dp = repository.findById(id);
        if (dp == null) {
            throw new NotFoundException("Dados pessoais não encontrados");
        }
        autorizarDonoOuGestor(dp);
        return toResponse(dp, incluirDocumento);
    }

    // ---------- Uploads ----------

    @Transactional
    public DadosPessoaisResponseDTO uploadFoto(Long id, byte[] bytes, String contentType) {
        storageValidator.validarImagem(bytes, contentType);
        DadosPessoais dp = repository.findById(id);
        if (dp == null) {
            throw new NotFoundException("Dados pessoais não encontrados");
        }
        autorizarDonoOuGestor(dp);

        String ext = extensaoPorContentType(contentType);
        String key = String.format("empresa-%d/dados-%d.%s", dp.getEmpresa().getId(), dp.getId(), ext);
        storageService.upload(bucketCredenciaisFoto, key, bytes, contentType);
        dp.setFotoObjectKey(key);
        LOG.infof("Foto de credencial enviada para dados pessoais id=%d", dp.getId());
        return toResponse(dp);
    }

    @Transactional
    public DadosPessoaisResponseDTO uploadDocumentoFoto(Long id, byte[] bytes, String contentType) {
        storageValidator.validarImagem(bytes, contentType);
        DadosPessoais dp = repository.findById(id);
        if (dp == null) {
            throw new NotFoundException("Dados pessoais não encontrados");
        }
        autorizarDonoOuGestor(dp);

        String ext = extensaoPorContentType(contentType);
        String key = String.format("empresa-%d/dados-%d-doc.%s", dp.getEmpresa().getId(), dp.getId(), ext);
        storageService.upload(bucketDocumentos, key, bytes, contentType);
        dp.setDocumentoFotoObjectKey(key);
        LOG.infof("Foto de documento enviada para dados pessoais id=%d", dp.getId());
        return toResponse(dp);
    }

    // ---------- converter ----------

    private DadosPessoaisResponseDTO toResponse(DadosPessoais dp) {
        return toResponse(dp, false);
    }

    private DadosPessoaisResponseDTO toResponse(DadosPessoais dp, boolean incluirDocumentoCompleto) {
        String fotoUrl = dp.getFotoObjectKey() == null ? null
                : storageService.downloadUrl(bucketCredenciaisFoto, dp.getFotoObjectKey());
        String documentoFotoUrl = dp.getDocumentoFotoObjectKey() == null ? null
                : storageService.downloadUrl(bucketDocumentos, dp.getDocumentoFotoObjectKey());
        String documentoExibido = incluirDocumentoCompleto && ehGestor()
                ? dp.getDocumento()
                : mascarar(dp.getTipoDocumento(), dp.getDocumento());
        return new DadosPessoaisResponseDTO(
                dp.getId(),
                dp.getNomeCompleto(),
                dp.getTipoDocumento(),
                documentoExibido,
                dp.getDataNascimento(),
                fotoUrl,
                documentoFotoUrl);
    }

    private String extensaoPorContentType(String contentType) {
        if (contentType == null) return "bin";
        String ct = contentType.toLowerCase();
        if (ct.contains("jpeg") || ct.contains("jpg")) return "jpg";
        if (ct.contains("png")) return "png";
        if (ct.contains("webp")) return "webp";
        return "bin";
    }
}
