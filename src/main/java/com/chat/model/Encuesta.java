package com.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "encuesta")
public class Encuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int envcuestaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creador_encuesta_id", nullable = false)
    private Usuario creadorEncuesta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chatId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mensaje_id")
    private Mensaje mensaje;

    @Column(nullable = false, length = 255)
    private String pregunta;

    @Column(nullable = false)
    private boolean anonima;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public int getEnvcuestaId() { return envcuestaId; }
    public Usuario getCreadorEncuesta() { return creadorEncuesta; }
    public void setCreadorEncuesta(Usuario creadorEncuesta) { this.creadorEncuesta = creadorEncuesta; }
    public Chat getChatId() { return chatId; }
    public void setChatId(Chat chatId) { this.chatId = chatId; }
    public Mensaje getMensaje() { return mensaje; }
    public void setMensaje(Mensaje mensaje) { this.mensaje = mensaje; }
    public String getPregunta() { return pregunta; }

    public void setPregunta(String pregunta) { this.pregunta = pregunta; }
    public boolean isAnonima() { return anonima; }
    public void setAnonima(boolean anonima) { this.anonima = anonima; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

}
