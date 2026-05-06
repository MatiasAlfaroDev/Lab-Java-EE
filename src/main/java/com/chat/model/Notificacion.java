package com.chat.model;

import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;

import org.hibernate.annotations.Collate;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "notificacion")
public class Notificacion {
    
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int notificacionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_receptor_id", nullable = false)
    private Usuario usuarioId;// Se mantiene 'emisor' según tu UML (aunque sea el receptor del estado)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    @Column(nullable = false)
    private String informacion;

    @Column(nullable = false)
    private LocalDateTime fechaNotificacion;

    @PrePersist
    protected void onCreate() {
        this.fechaNotificacion = LocalDateTime.now();
    }

    public int getNotificacionId() { return notificacionId; }
    public Usuario getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Usuario usuarioId) { this.usuarioId = usuarioId; }
    public Mensaje getMensaje() { return mensaje; }
    public void setMensaje(Mensaje mensaje) { this.mensaje = mensaje; }
    public String getInformacion() { return informacion; }
    public void setInformacion(String informacion) { this.informacion = informacion; }
    public LocalDateTime getFechaNotificacion() { return fechaNotificacion; }
}
