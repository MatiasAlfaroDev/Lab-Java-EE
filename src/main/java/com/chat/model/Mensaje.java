package com.chat.model;

import com.chat.datatype.DtFecha;
import com.chat.enums.EstadoMensaje;
import jakarta.persistence.*;

@Entity
@Table(name = "mensaje")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "dia",  column = @Column(name = "fecha_enviado_dia")),
        @AttributeOverride(name = "mes",  column = @Column(name = "fecha_enviado_mes")),
        @AttributeOverride(name = "anio", column = @Column(name = "fecha_enviado_anio"))
    })
    private DtFecha fechaEnviado;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "dia",  column = @Column(name = "fecha_entregado_dia")),
        @AttributeOverride(name = "mes",  column = @Column(name = "fecha_entregado_mes")),
        @AttributeOverride(name = "anio", column = @Column(name = "fecha_entregado_anio"))
    })
    private DtFecha fechaEntregado;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "dia",  column = @Column(name = "fecha_leido_dia")),
        @AttributeOverride(name = "mes",  column = @Column(name = "fecha_leido_mes")),
        @AttributeOverride(name = "anio", column = @Column(name = "fecha_leido_anio"))
    })
    private DtFecha fechaLeido;

    @Column(nullable = false)
    private boolean eliminado;

    @Column(nullable = false)
    private boolean editado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMensaje estado;

    public int getId() { return id; }
    public Usuario getEmisor() { return emisor; }
    public void setEmisor(Usuario emisor) { this.emisor = emisor; }
    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public DtFecha getFechaEnviado() { return fechaEnviado; }
    public void setFechaEnviado(DtFecha fechaEnviado) { this.fechaEnviado = fechaEnviado; }
    public DtFecha getFechaEntregado() { return fechaEntregado; }
    public void setFechaEntregado(DtFecha fechaEntregado) { this.fechaEntregado = fechaEntregado; }
    public DtFecha getFechaLeido() { return fechaLeido; }
    public void setFechaLeido(DtFecha fechaLeido) { this.fechaLeido = fechaLeido; }
    public boolean isEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }
    public boolean isEditado() { return editado; }
    public void setEditado(boolean editado) { this.editado = editado; }
    public EstadoMensaje getEstado() { return estado; }
    public void setEstado(EstadoMensaje estado) { this.estado = estado; }
}
