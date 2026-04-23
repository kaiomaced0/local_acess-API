package ka.mdo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Forma geométrica (polígono, retângulo ou círculo) que representa um
 * {@link EspacoEvento} dentro de um {@link MapaEvento} (atividade 040).
 *
 * <p>A geometria em si é serializada como JSON em {@link #geometriaJson}
 * por conta da heterogeneidade — não compensa normalizar em colunas/linhas.
 * O serviço {@code MapaEventoService} valida o payload antes de persistir.
 *
 * <p>A cor é armazenada no formato HTML {@code #RRGGBB} (7 chars) e
 * validada por regex no service. O {@code rotulo} é opcional e usado pelo
 * frontend para exibir legenda sobre a forma (quando o nome do espaço é
 * longo).
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class FormaMapa extends EntityClass {

    @ManyToOne(optional = false)
    @JoinColumn(name = "mapa_id", nullable = false)
    private MapaEvento mapa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "espaco_id", nullable = false)
    private EspacoEvento espaco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoForma tipo;

    @Column(name = "geometriaJson", columnDefinition = "TEXT", nullable = false)
    private String geometriaJson;

    @Column(name = "corHex", nullable = false, length = 7)
    private String corHex;

    @Column(length = 100)
    private String rotulo;

    public MapaEvento getMapa() {
        return mapa;
    }

    public void setMapa(MapaEvento mapa) {
        this.mapa = mapa;
    }

    public EspacoEvento getEspaco() {
        return espaco;
    }

    public void setEspaco(EspacoEvento espaco) {
        this.espaco = espaco;
    }

    public TipoForma getTipo() {
        return tipo;
    }

    public void setTipo(TipoForma tipo) {
        this.tipo = tipo;
    }

    public String getGeometriaJson() {
        return geometriaJson;
    }

    public void setGeometriaJson(String geometriaJson) {
        this.geometriaJson = geometriaJson;
    }

    public String getCorHex() {
        return corHex;
    }

    public void setCorHex(String corHex) {
        this.corHex = corHex;
    }

    public String getRotulo() {
        return rotulo;
    }

    public void setRotulo(String rotulo) {
        this.rotulo = rotulo;
    }
}
