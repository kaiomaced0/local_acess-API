package ka.mdo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Filter;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
public class Usuario extends EntityClass {

    @NotNull
    @NotBlank
    @Size(min = 3, max = 70)
    private String nome;
    @NotNull
    @NotBlank
    @Size(min = 8, max = 200)
    @Email(message = "email inválido")
    private String email;
    @NotNull
    @NotBlank
    private String senha;
    @NotNull
    @NotBlank
    @CPF(message = "cpf inválido")
    private String cpf;

    private LocalDate dataNascimento;

    private String telefone;

    private String naturalidade;

    @ElementCollection
    @CollectionTable(name = "usuario_perfil", joinColumns = @JoinColumn(name = "id_usuario", referencedColumnName = "id"))
    @Column(name = "perfil", length = 30)
    @Enumerated(EnumType.STRING)
    private Set<Perfil> perfis;

    /**
     * Canais em que o usuário deseja receber notificações (atividade 032).
     * Default aplicado na migração V9: {@code [WEBSOCKET, EMAIL]}.
     * PUSH fica fora do default enquanto FCM é apenas stub.
     */
    @ElementCollection
    @CollectionTable(name = "usuario_canais_notificacao",
            joinColumns = @JoinColumn(name = "usuario_id", referencedColumnName = "id"))
    @Column(name = "canal", length = 20)
    @Enumerated(EnumType.STRING)
    private Set<CanalNotificacao> canaisNotificacao;

    @OneToMany
    @JoinColumn(name = "usuario_ingresso")
    private List<Ingresso> ingressos;

    /**
     * Dados pessoais estendidos do usuário (nome completo, documento tipado,
     * foto). Usado quando {@code Evento.exigeDadosPessoais == true}. Atividade 020.
     * Relação 1:1 com FK do lado Usuario para simplificar navegação a partir
     * do dono da credencial em {@code AcessoService}.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dados_pessoais_id")
    private DadosPessoais dadosPessoais;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public List<Ingresso> getIngressos() {
        return ingressos;
    }

    public void setIngressos(List<Ingresso> ingressos) {
        this.ingressos = ingressos;
    }

    public Set<Perfil> getPerfis() {
        return perfis;
    }

    public void setPerfis(Set<Perfil> perfis) {
        this.perfis = perfis;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getNaturalidade() {
        return naturalidade;
    }

    public void setNaturalidade(String naturalidade) {
        this.naturalidade = naturalidade;
    }

    public DadosPessoais getDadosPessoais() {
        return dadosPessoais;
    }

    public void setDadosPessoais(DadosPessoais dadosPessoais) {
        this.dadosPessoais = dadosPessoais;
    }

    public Set<CanalNotificacao> getCanaisNotificacao() {
        return canaisNotificacao;
    }

    public void setCanaisNotificacao(Set<CanalNotificacao> canaisNotificacao) {
        this.canaisNotificacao = canaisNotificacao;
    }
}
