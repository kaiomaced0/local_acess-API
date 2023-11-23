package ka.mdo.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Evento extends EntityClass{
    private String nome;

    private String descricao;

    private String local;

    private LocalDateTime inicioEvento;

    private LocalDateTime finalEvento;

    @OneToMany
    @JoinColumn(name = "evento_espacoevento")
    private List<EspacoEvento> espacoEventos;

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public LocalDateTime getInicioEvento() {
        return inicioEvento;
    }

    public void setInicioEvento(LocalDateTime inicioEvento) {
        this.inicioEvento = inicioEvento;
    }

    public LocalDateTime getFinalEvento() {
        return finalEvento;
    }

    public void setFinalEvento(LocalDateTime finalEvento) {
        this.finalEvento = finalEvento;
    }

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
