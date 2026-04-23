package ka.mdo.model;

/**
 * Tipo geométrico de uma {@link FormaMapa} no mapa 2D do evento
 * (atividade 040). A estrutura esperada em {@code geometriaJson} varia por
 * tipo — ver {@code MapaEventoService.validarGeometria}:
 *
 * <ul>
 *   <li>{@link #RETANGULO}: {@code {"x":int,"y":int,"w":int,"h":int}}</li>
 *   <li>{@link #CIRCULO}:   {@code {"cx":int,"cy":int,"r":int}}</li>
 *   <li>{@link #POLIGONO}:  {@code {"pontos":[[x1,y1],[x2,y2],...]}}</li>
 * </ul>
 */
public enum TipoForma {
    RETANGULO,
    POLIGONO,
    CIRCULO
}
