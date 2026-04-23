package ka.mdo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapa 2D de um {@link Evento} com polígonos/retângulos/círculos coloridos
 * representando cada {@link EspacoEvento} (atividade 040).
 *
 * <p>Um evento tem no máximo um mapa (relacionamento 1:1 via
 * {@code evento_id UNIQUE}). A criação do mapa é opcional — eventos podem
 * operar sem representação visual.
 *
 * <p>Coordenadas das formas são armazenadas como JSON serializado em
 * {@link FormaMapa#getGeometriaJson()}. O {@code unidade} é metadado livre
 * ("px", "m", "ft") usado apenas pelo frontend para rotular a escala.
 *
 * <p>A imagem de fundo (opcional) fica armazenada no bucket {@code mapas}
 * — ver {@code storage.bucket.mapas} e {@code StorageBucketBootstrap}.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
public class MapaEvento extends EntityClass {

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @OneToOne(optional = false)
    @JoinColumn(name = "evento_id", nullable = false, unique = true)
    private Evento evento;

    @Column(nullable = false)
    private int largura = 1000;

    @Column(nullable = false)
    private int altura = 1000;

    @Column(nullable = false, length = 20)
    private String unidade = "px";

    /**
     * Chave do objeto no bucket {@code mapas} quando há imagem de fundo.
     * Null quando o mapa usa apenas polígonos sobre fundo em branco.
     */
    @Column(length = 500)
    private String imagemFundoObjectKey;

    @OneToMany(mappedBy = "mapa", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FormaMapa> formas = new ArrayList<>();

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public int getLargura() {
        return largura;
    }

    public void setLargura(int largura) {
        this.largura = largura;
    }

    public int getAltura() {
        return altura;
    }

    public void setAltura(int altura) {
        this.altura = altura;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public String getImagemFundoObjectKey() {
        return imagemFundoObjectKey;
    }

    public void setImagemFundoObjectKey(String imagemFundoObjectKey) {
        this.imagemFundoObjectKey = imagemFundoObjectKey;
    }

    public List<FormaMapa> getFormas() {
        return formas;
    }

    public void setFormas(List<FormaMapa> formas) {
        this.formas = formas;
    }
}
