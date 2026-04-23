package ka.mdo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

/**
 * Notificação persistida emitida pelo {@code NotificacaoService} (atividade 032).
 *
 * <p>É o registro canônico exibido no painel do usuário — cada canal
 * (websocket, email, push) apenas *entrega* uma cópia; a fonte da verdade é
 * esta tabela. A criação é síncrona e dentro da transação do chamador; a
 * entrega por canal é disparada por evento CDI assíncrono
 * ({@code NotificacaoCriada}) em transação separada.
 *
 * <ul>
 *     <li>{@link #empresa} NOT NULL — padrão multitenant (filtro Hibernate
 *     {@code tenantFilter}).</li>
 *     <li>{@link #destinatario} NOT NULL — a quem a notificação se dirige
 *     (sempre 1 único usuário).</li>
 *     <li>{@link #payloadJson} — metadata livre em JSON (ex.: id da pendência
 *     quando {@code tipo == PENDENCIA_ABERTA}); pode ser {@code null}.</li>
 * </ul>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "tenantFilter", condition = "empresa_id = :empresaId")
public class Notificacao extends EntityClass {

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoNotificacao tipo;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "mensagem", nullable = false, length = 500)
    private String mensagem;

    // TEXT pois é JSON arbitrário — tamanho imprevisível.
    @Column(name = "payloadJson", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "lida", nullable = false)
    private boolean lida = false;

    @Column(name = "criadaEm", nullable = false)
    private LocalDateTime criadaEm;

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Usuario destinatario) {
        this.destinatario = destinatario;
    }

    public TipoNotificacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoNotificacao tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public boolean isLida() {
        return lida;
    }

    public void setLida(boolean lida) {
        this.lida = lida;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }
}
