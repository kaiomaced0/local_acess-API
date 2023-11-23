package ka.mdo.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class TipoIngresso extends EntityClass{

    private String nome;

    @ManyToMany
    @JoinColumn(name = "tipoingresso_espacoevento")
    private List<EspacoEvento> espacoEventos;

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
}
