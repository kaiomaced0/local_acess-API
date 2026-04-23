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
 * Log de auditoria das mudanças de whitelist de {@link TipoIngresso} por
 * {@link EspacoEvento} (atividade 030). Só persiste — a atividade 041 vai
 * montar dashboards em cima.
 *
 * <p>Para ADICIONADO/REMOVIDO, {@code tipoIngressoId} referencia o
 * {@link TipoIngresso} afetado. Para SUBSTITUIDO, o campo fica nulo (o PUT
 * substitui a lista inteira — histórico do conteúdo novo/antigo não cabe
 * aqui; se precisar, atividade 041 cruza com os ADICIONADO/REMOVIDO
 * seguintes ou com um snapshot).
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
public class AutorizacaoAuditoria extends EntityClass {

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "espaco_evento_id", nullable = false)
    private EspacoEvento espaco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcaoAutorizacao acao;

    /** Nullable: só preenchido em ADICIONADO/REMOVIDO. */
    @Column(name = "tipo_ingresso_id")
    private Long tipoIngressoId;

    /** {@code sub}/{@code usuarioId} do JWT que fez a mudança. */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public EspacoEvento getEspaco() {
        return espaco;
    }

    public void setEspaco(EspacoEvento espaco) {
        this.espaco = espaco;
    }

    public AcaoAutorizacao getAcao() {
        return acao;
    }

    public void setAcao(AcaoAutorizacao acao) {
        this.acao = acao;
    }

    public Long getTipoIngressoId() {
        return tipoIngressoId;
    }

    public void setTipoIngressoId(Long tipoIngressoId) {
        this.tipoIngressoId = tipoIngressoId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}
