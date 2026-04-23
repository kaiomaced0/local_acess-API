package ka.mdo.model;

import java.time.LocalDate;

import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;

/**
 * Dados pessoais do dono de uma credencial.
 *
 * <p>Decisão (atividade 020): vinculamos 1:1 ao {@link Usuario} — um usuário
 * pode ter várias credenciais ({@code Usuario.ingressos}), então reaproveitar
 * os mesmos dados pessoais evita duplicação. Quando um evento exige dados
 * pessoais ({@code Evento.exigeDadosPessoais}), o {@code AcessoService} verifica
 * se o usuário dono da credencial tem um {@code DadosPessoais} preenchido.
 *
 * <p><b>Privacidade (LGPD)</b>: o {@code documento} é persistido em claro;
 * a garantia de criptografia em repouso é dada pela camada de storage
 * (banco com TDE em produção; bucket com SSE). Nunca logar
 * {@code nomeCompleto} ou {@code documento} em claro — o
 * {@code DadosPessoaisResponseDTO} retorna o documento mascarado por default.
 *
 * <p><b>Fotos</b>: armazenadas em bucket separado (`credenciais-foto` para a
 * selfie, `documentos` para foto do documento). Persistimos apenas a chave
 * do objeto; URLs pré-assinadas são geradas on-demand via {@code StorageService}.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
@Table(
        name = "DadosPessoais",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dadospessoais_empresa_tipo_doc",
                        columnNames = {"empresa_id", "tipoDocumento", "documento"})
        },
        indexes = {
                @Index(name = "ix_dadospessoais_empresa_documento",
                        columnList = "empresa_id,documento")
        }
)
public class DadosPessoais extends EntityClass {

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 150)
    private String nomeCompleto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoDocumento", nullable = false, length = 20)
    private TipoDocumento tipoDocumento;

    /**
     * Documento já normalizado (para CPF: apenas dígitos, 11 chars).
     * Nunca logar em claro. Retornado mascarado por default nas respostas.
     */
    @Column(nullable = false, length = 40)
    private String documento;

    private LocalDate dataNascimento;

    /**
     * Chave (nome do objeto) no bucket {@code credenciais-foto}. Pode ser
     * null enquanto o usuário não fez upload da selfie. Nunca URL pública —
     * URL assinada é gerada sob demanda via {@code StorageService#downloadUrl}.
     */
    @Column(length = 255)
    private String fotoObjectKey;

    /**
     * Chave no bucket {@code documentos} para a foto do documento (RG/CPF
     * físico/passaporte). Opcional mesmo quando o evento exige dados pessoais
     * (regra atual: bastam nome + documento + foto da pessoa).
     */
    @Column(length = 255)
    private String documentoFotoObjectKey;

    /**
     * Usuário dono destes dados. {@code @OneToOne} com FK do lado {@link Usuario}
     * (campo {@code Usuario.dadosPessoais}); aqui só expomos o lado inverso.
     */
    @OneToOne(mappedBy = "dadosPessoais", optional = true)
    private Usuario usuario;

    /**
     * Atividade 021: true quando o rosto do dono já foi enrolado no Frigate
     * (primeira leitura com sucesso em um evento com {@code validarFacial}).
     * Leituras seguintes só comparam, não re-enrolam.
     */
    @Column(nullable = false)
    private boolean rostoFrigateCadastrado = false;

    /**
     * Atividade 021: identificador do rosto/pessoa no Frigate. Nulo enquanto
     * ainda não houve enrolamento. Mantemos separado do {@code Usuario.id}
     * para desacoplar do nosso esquema interno — o Frigate usa strings.
     */
    @Column(length = 100)
    private String frigatePessoaId;

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getFotoObjectKey() {
        return fotoObjectKey;
    }

    public void setFotoObjectKey(String fotoObjectKey) {
        this.fotoObjectKey = fotoObjectKey;
    }

    public String getDocumentoFotoObjectKey() {
        return documentoFotoObjectKey;
    }

    public void setDocumentoFotoObjectKey(String documentoFotoObjectKey) {
        this.documentoFotoObjectKey = documentoFotoObjectKey;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public boolean isRostoFrigateCadastrado() {
        return rostoFrigateCadastrado;
    }

    public void setRostoFrigateCadastrado(boolean rostoFrigateCadastrado) {
        this.rostoFrigateCadastrado = rostoFrigateCadastrado;
    }

    public String getFrigatePessoaId() {
        return frigatePessoaId;
    }

    public void setFrigatePessoaId(String frigatePessoaId) {
        this.frigatePessoaId = frigatePessoaId;
    }

    /**
     * Indica se os dados estão completos para liberar acesso em um evento
     * que exige dados pessoais. Regra: nome, documento e foto são obrigatórios.
     */
    public boolean isCompleto() {
        return nomeCompleto != null && !nomeCompleto.isBlank()
                && documento != null && !documento.isBlank()
                && fotoObjectKey != null && !fotoObjectKey.isBlank();
    }
}
