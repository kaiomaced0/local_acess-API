package ka.mdo.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

// EnumType/Enumerated importados via jakarta.persistence.*

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
public class Ingresso extends EntityClass{

    private String chaveAcesso;

    private String lote;

    /**
     * Token opaco, cripto-seguro, único por credencial. Usado para gerar QR Code
     * e para validação nos aparelhos de leitura. Nunca retornado em listagens
     * públicas nem logado.
     */
    @Column(name = "token", unique = true, nullable = false, length = 64)
    private String token;

    @JoinColumn(name = "ingresso_tipoingresso")
    @ManyToOne
    private TipoIngresso tipoIngresso;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /**
     * Flag de credencial global (atividade 033). Quando preenchida, o
     * {@link ka.mdo.service.AcessoService} curto-circuita parte das
     * validações de acesso — ver {@link EscopoGlobal} para o detalhe de
     * cada valor. Default {@code null} = credencial comum (fluxo clássico).
     *
     * <p>Decisão (033): flag fica em {@code Ingresso}, não em {@code Usuario}.
     * Assim o gestor pode emitir uma credencial específica com acesso
     * global (ex.: bracelete de staff para um único evento) sem promover
     * todas as credenciais do portador — e pode revogar o acesso global
     * invalidando apenas aquela credencial.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "escopoGlobal", length = 20)
    private EscopoGlobal escopoGlobal;

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public void setChaveAcesso(String chaveAcesso) {
        this.chaveAcesso = chaveAcesso;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public TipoIngresso getTipoIngresso() {
        return tipoIngresso;
    }

    public void setTipoIngresso(TipoIngresso tipoIngresso) {
        this.tipoIngresso = tipoIngresso;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public EscopoGlobal getEscopoGlobal() {
        return escopoGlobal;
    }

    public void setEscopoGlobal(EscopoGlobal escopoGlobal) {
        this.escopoGlobal = escopoGlobal;
    }
}
