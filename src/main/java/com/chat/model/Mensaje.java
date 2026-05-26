package com.chat.model;

import com.chat.enums.EstadoMensaje;
import com.chat.enums.TipoMensaje;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "mensaje")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMensaje tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMensaje estado;

    // Relación opcional para mensajes que son respuestas a otros mensajes
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mensaje_referencia_id")
    private Mensaje mensajeReferencia;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "fecha_enviado", nullable = false, updatable = false)
    private Instant fechaEnviado;
  
    @Column(nullable = false)
    private boolean editado;

    @PrePersist
    protected void onCreate() {
        this.fechaEnviado = Instant.now();
        this.editado = false;
    }


    public int getId() { return id; }

    public Usuario getEmisor() { return emisor; }
    public void setEmisor(Usuario emisor) { this.emisor = emisor; }
    
    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }
    
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    
    public Instant getFechaEnviado() { return fechaEnviado; }
    public void setFechaEnviado(Instant fechaEnviado) { this.fechaEnviado = fechaEnviado; }    
    public boolean isEditado() { return editado; }
    public void setEditado(boolean editado) { this.editado = editado; }
    
    public TipoMensaje getTipo() { return tipo; }
    public void setTipo(TipoMensaje tipo) { this.tipo = tipo; }
    
    public EstadoMensaje getEstado() { return estado; }
    public void setEstado(EstadoMensaje estado) { this.estado = estado; }
}
