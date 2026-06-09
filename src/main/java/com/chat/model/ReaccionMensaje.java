package com.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "reaccion_mensaje",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {
                "mensaje",
                "usuario_reaccion_id"
            }
        )
    })
public class ReaccionMensaje {
   
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int reaccionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mensaje")
    private Mensaje mensaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_reaccion_id")   
    private Usuario usuarioReaccion;

    @Column(nullable = false, length = 50)
    private String  emojiString; // Ejemplo: "me gusta", "me encanta",
    
    @Column(name = "fecha_reaccion", nullable = false, updatable = false)
    private LocalDateTime fechaReaccion;
    
    @PrePersist
    protected void onCreate() {
        this.fechaReaccion = LocalDateTime.now();
    }   

    public int getReaccionId() { return reaccionId; }
    public Mensaje getMensaje() { return mensaje; }
    public void setMensaje(Mensaje mensaje) { this.mensaje = mensaje; }
    public Usuario getUsuarioReaccion() { return usuarioReaccion; }
    public void setUsuarioReaccion(Usuario usuarioReaccion) { this.usuarioReaccion = usuarioReaccion; }
    public String getEmojiString() { return emojiString; }
    public void setEmojiString(String emojiString) { this.emojiString = emojiString; }
    public LocalDateTime getFechaReaccion() { return fechaReaccion; }
}
