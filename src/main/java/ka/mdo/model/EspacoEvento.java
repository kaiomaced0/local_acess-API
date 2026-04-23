package ka.mdo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Filter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
public class EspacoEvento extends EntityClass {

    private String nome;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /**
     * Whitelist de {@link TipoIngresso} autorizados a acessar este local
     * (atividade 030). Relacionamento N:N com tabela de associação
     * {@code espacoevento_tipo_ingresso_autorizado}.
     *
     * <p>Política adotada quando vazia: {@code AcessoService} trata a lista
     * vazia como "sem restrição" (autoriza). Ver justificativa no resultado
     * da atividade.
     *
     * <p><b>Fetch</b>: LAZY para evitar N+1 em listagens de espaços. Carregue
     * explicitamente (JOIN FETCH ou dentro da transação) quando precisar
     * iterar.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "espacoevento_tipo_ingresso_autorizado",
            joinColumns = @JoinColumn(name = "espaco_evento_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_ingresso_id"))
    private Set<TipoIngresso> tiposIngressoAutorizados = new HashSet<>();

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Set<TipoIngresso> getTiposIngressoAutorizados() {
        return tiposIngressoAutorizados;
    }

    public void setTiposIngressoAutorizados(Set<TipoIngresso> tiposIngressoAutorizados) {
        this.tiposIngressoAutorizados = tiposIngressoAutorizados;
    }
}
