package ka.mdo.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
public class TipoIngresso extends EntityClass{

    private String nome;

    @ManyToMany
    @JoinColumn(name = "tipoingresso_espacoevento")
    private List<EspacoEvento> espacoEventos;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public List<EspacoEvento> getEspacoEventos() {
        return espacoEventos;
    }

    public void setEspacoEventos(List<EspacoEvento> espacoEventos) {
        this.espacoEventos = espacoEventos;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
}
