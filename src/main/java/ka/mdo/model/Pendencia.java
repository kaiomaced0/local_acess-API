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
 * Pendência de acesso (atividade 031).
 *
 * <p>Criada quando {@link ka.mdo.service.AcessoService} decide
 * {@link ResultadoAcesso#PENDENTE} — três cenários hoje:
 * <ul>
 *     <li>{@code DADOS_PESSOAIS_INCOMPLETOS} (020) — evento exige dados e o
 *     dono da credencial não preencheu. {@link #fotoCapturadaObjectKey} fica
 *     {@code null}.</li>
 *     <li>{@code ROSTO_DIVERGENTE} (021) — comparação facial abaixo do
 *     threshold. Guardamos a captura no bucket para o gestor conferir.</li>
 *     <li>{@code FRIGATE_INDISPONIVEL} (021) — política de fallback
 *     "pendente".</li>
 * </ul>
 *
 * <p>Multitenant: {@link #empresa} NOT NULL + filtro Hibernate
 * {@code tenantFilter}.
 *
 * <p>Idempotência (ver {@link ka.mdo.pendencia.PendenciaService#criar}):
 * se já existe uma pendência {@link StatusPendencia#ABERTA} para a mesma
 * credencial e mesmo motivo, apenas atualizamos
 * {@link #fotoCapturadaObjectKey} — não duplicamos registro nem disparamos
 * nova notificação.
 *
 * <p>Índices (migração V13):
 * <ul>
 *     <li>{@code (empresa_id, status, criadaEm DESC)} — fila do gestor.</li>
 *     <li>{@code (credencial_id, status)} — checagem de duplicidade +
 *     histórico por credencial.</li>
 * </ul>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
public class Pendencia extends EntityClass {

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "credencial_id", nullable = false)
    private Ingresso credencial;

    /**
     * {@link EspacoEvento} em que a leitura ocorreu. Nullable porque aparelhos
     * de entrada geral do evento operam sem local específico.
     */
    @ManyToOne
    @JoinColumn(name = "local_id")
    private EspacoEvento local;

    @ManyToOne
    @JoinColumn(name = "aparelho_id", nullable = false)
    private Aparelho aparelho;

    /**
     * Código curto e estável (ex.: {@code ROSTO_DIVERGENTE}, {@code
     * DADOS_PESSOAIS_INCOMPLETOS}). Mesmo vocabulário usado em
     * {@link LogAcesso#getMotivo()}.
     */
    @Column(name = "motivo", nullable = false, length = 100)
    private String motivo;

    /**
     * Chave da foto capturada no bucket {@code capturas-acesso}. Pode ser
     * {@code null} (ex.: pendência de dados pessoais, onde não há foto).
     * A URL assinada é gerada on-demand via {@code StorageService#downloadUrl}.
     */
    @Column(name = "fotoCapturadaObjectKey", length = 500)
    private String fotoCapturadaObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPendencia status = StatusPendencia.ABERTA;

    @Column(name = "criadaEm", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "resolvidaEm")
    private LocalDateTime resolvidaEm;

    @ManyToOne
    @JoinColumn(name = "resolvida_por_id")
    private Usuario resolvidaPor;

    @Column(name = "observacaoResolucao", length = 500)
    private String observacaoResolucao;

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Ingresso getCredencial() {
        return credencial;
    }

    public void setCredencial(Ingresso credencial) {
        this.credencial = credencial;
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

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getFotoCapturadaObjectKey() {
        return fotoCapturadaObjectKey;
    }

    public void setFotoCapturadaObjectKey(String fotoCapturadaObjectKey) {
        this.fotoCapturadaObjectKey = fotoCapturadaObjectKey;
    }

    public StatusPendencia getStatus() {
        return status;
    }

    public void setStatus(StatusPendencia status) {
        this.status = status;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }

    public LocalDateTime getResolvidaEm() {
        return resolvidaEm;
    }

    public void setResolvidaEm(LocalDateTime resolvidaEm) {
        this.resolvidaEm = resolvidaEm;
    }

    public Usuario getResolvidaPor() {
        return resolvidaPor;
    }

    public void setResolvidaPor(Usuario resolvidaPor) {
        this.resolvidaPor = resolvidaPor;
    }

    public String getObservacaoResolucao() {
        return observacaoResolucao;
    }

    public void setObservacaoResolucao(String observacaoResolucao) {
        this.observacaoResolucao = observacaoResolucao;
    }
}
