package ka.mdo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Filter;

/**
 * Vínculo N:N entre um {@link Usuario} (perfil {@link Perfil#GESTOR_LOCAL}) e
 * um {@link EspacoEvento} que ele gerencia. Introduzido pela atividade 041
 * para fechar os débitos das 030 e 031.
 *
 * <ul>
 *     <li>030: {@code GESTOR_LOCAL} agora enxerga apenas as autorizações dos
 *     locais vinculados a ele.</li>
 *     <li>031: a fila de pendências e as notificações do
 *     {@link ka.mdo.pendencia.PendenciaService} respeitam o vínculo — só
 *     gestores vinculados ao local da pendência recebem notificação.</li>
 *     <li>041: as métricas/dashboards filtram automaticamente por esses
 *     locais quando o chamador é {@code GESTOR_LOCAL}.</li>
 * </ul>
 *
 * <p><b>Multitenancy</b>: {@link #empresa} NOT NULL + filtro {@code tenantFilter}.
 *
 * <p><b>Unicidade</b>: {@code (usuario_id, espaco_evento_id)} é único — não
 * permitimos duplicar o vínculo entre um mesmo par.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
@Table(name = "GestorLocal",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_gestorlocal_usuario_local",
                columnNames = {"usuario_id", "espaco_evento_id"}))
public class GestorLocal extends EntityClass {

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario gestor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "espaco_evento_id", nullable = false)
    private EspacoEvento local;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    public Usuario getGestor() {
        return gestor;
    }

    public void setGestor(Usuario gestor) {
        this.gestor = gestor;
    }

    public EspacoEvento getLocal() {
        return local;
    }

    public void setLocal(EspacoEvento local) {
        this.local = local;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
}
