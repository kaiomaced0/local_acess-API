package ka.mdo.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
public class Evento extends EntityClass{
    private String nome;

    private String descricao;

    private String local;

    private LocalDateTime inicioEvento;

    private LocalDateTime finalEvento;

    @OneToMany
    @JoinColumn(name = "evento_espacoevento")
    private List<EspacoEvento> espacoEventos;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /**
     * Quando true, o {@code AcessoService} exige que o dono da credencial
     * tenha {@link DadosPessoais} completos (nome, documento e foto).
     * Credenciais sem dados retornam {@code PENDENTE} com motivo
     * {@code DADOS_PESSOAIS_INCOMPLETOS} (atividade 020).
     */
    @Column(nullable = false)
    private boolean exigeDadosPessoais = false;

    /**
     * Quando true, o {@code AcessoService} exige validação facial a cada leitura
     * (atividade 021). A primeira leitura cadastra o rosto do dono no Frigate;
     * leituras seguintes comparam — divergência &gt; threshold vira
     * {@code PENDENTE} com motivo {@code ROSTO_DIVERGENTE}. Se o aparelho não
     * enviar a foto capturada, resposta é {@code PENDENTE} com motivo
     * {@code FOTO_FACIAL_AUSENTE}.
     */
    @Column(nullable = false)
    private boolean validarFacial = false;

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

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public boolean isExigeDadosPessoais() {
        return exigeDadosPessoais;
    }

    public void setExigeDadosPessoais(boolean exigeDadosPessoais) {
        this.exigeDadosPessoais = exigeDadosPessoais;
    }

    public boolean isValidarFacial() {
        return validarFacial;
    }

    public void setValidarFacial(boolean validarFacial) {
        this.validarFacial = validarFacial;
    }
}
