package ka.mdo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

/**
 * Registro de auditoria de cada decisão tomada por um {@link Aparelho} ao
 * validar uma credencial. Persistido pela atividade 012 a partir de eventos
 * assíncronos disparados em {@code AcessoService}.
 *
 * <ul>
 *     <li>{@link #empresa} NOT NULL — padrão de tenant.</li>
 *     <li>{@link #ingresso} nullable — quando o token não existe, a
 *     credencial referenciada é nula (tentativa com QR inválido).</li>
 *     <li>{@link #local} nullable — {@code localId} é opcional no request
 *     (aparelho de entrada geral do evento).</li>
 *     <li>{@link #aparelho} NOT NULL — toda leitura vem de um aparelho.</li>
 *     <li>{@link #fotoCapturadaUrl} placeholder para a atividade 021
 *     (validação facial) — não integra com storage aqui.</li>
 * </ul>
 *
 * <p>Índices relevantes (migração V7):
 * <ul>
 *     <li>{@code (empresa_id, dataHora)} — dashboards por tenant.</li>
 *     <li>{@code (ingresso_id, dataHora)} — histórico por credencial.</li>
 * </ul>
 *
 * <p>TODO futuro: particionar por mês se volume exigir.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
public class LogAcesso extends EntityClass {

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "ingresso_id")
    private Ingresso ingresso;

    @ManyToOne
    @JoinColumn(name = "local_id")
    private EspacoEvento local;

    @ManyToOne
    @JoinColumn(name = "aparelho_id", nullable = false)
    private Aparelho aparelho;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private ResultadoAcesso resultado;

    @Column(name = "motivo", length = 100)
    private String motivo;

    @Column(name = "dataHora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "fotoCapturadaUrl", length = 500)
    private String fotoCapturadaUrl;

    /**
     * Atividade 033: {@code true} quando a decisão foi tomada via
     * credencial com {@link ka.mdo.model.EscopoGlobal} preenchido (curto-circuito
     * de checagens de perfil/local). Destaque para auditoria.
     */
    @Column(name = "acessoGlobal", nullable = false)
    private boolean acessoGlobal;

    /**
     * Atividade 041: entrada ou saída. Default {@link TipoMovimento#ENTRADA}
     * (leitores antigos que não enviam o campo preservam a semântica do 012).
     * Base do cálculo de ocupação por local em
     * {@link ka.mdo.repository.LogAcessoRepository#ocupacaoPorLocal}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipoMovimento", nullable = false, length = 20)
    private TipoMovimento tipoMovimento = TipoMovimento.ENTRADA;

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Ingresso getIngresso() {
        return ingresso;
    }

    public void setIngresso(Ingresso ingresso) {
        this.ingresso = ingresso;
    }

    public EspacoEvento getLocal() {
        return local;
    }

    public void setLocal(EspacoEvento local) {
        this.local = local;
    }

    public Aparelho getAparelho() {
        return aparelho;
    }

    public void setAparelho(Aparelho aparelho) {
        this.aparelho = aparelho;
    }

    public ResultadoAcesso getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoAcesso resultado) {
        this.resultado = resultado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getFotoCapturadaUrl() {
        return fotoCapturadaUrl;
    }

    public void setFotoCapturadaUrl(String fotoCapturadaUrl) {
        this.fotoCapturadaUrl = fotoCapturadaUrl;
    }

    public boolean isAcessoGlobal() {
        return acessoGlobal;
    }

    public void setAcessoGlobal(boolean acessoGlobal) {
        this.acessoGlobal = acessoGlobal;
    }

    public TipoMovimento getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimento tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }
}
