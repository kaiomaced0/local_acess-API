package ka.mdo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Filter;

/**
 * Dispositivo físico (totem, leitor, tablet) usado por um
 * {@link Perfil#OPERADOR_APARELHO} para validar credenciais na entrada do
 * evento ou de um {@link EspacoEvento} específico.
 *
 * <ul>
 *     <li>{@link #localEspecifico} nulo: aparelho opera na entrada do evento
 *     (controle geral), não restringe por espaço interno.</li>
 *     <li>{@link #localEspecifico} definido: aparelho controla o acesso a
 *     aquele espaço específico — apenas tipos de ingresso autorizados no
 *     local passam (regra da atividade 030).</li>
 *     <li>{@link #evento} opcional: permite vincular o aparelho a um evento
 *     único. Se nulo, o aparelho é genérico e pode operar em múltiplos
 *     eventos da mesma empresa.</li>
 * </ul>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
public class Aparelho extends EntityClass {

    @Column(length = 255)
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "local_especifico_id")
    private EspacoEvento localEspecifico;

    @ManyToOne
    @JoinColumn(name = "evento_id")
    private Evento evento;

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public EspacoEvento getLocalEspecifico() {
        return localEspecifico;
    }

    public void setLocalEspecifico(EspacoEvento localEspecifico) {
        this.localEspecifico = localEspecifico;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }
}
