package com.chat.model;

import java.time.LocalDateTime;
import com.chat.enums.TipoChat;
import jakarta.persistence.*;

@Entity
@Table(name = "chat")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int chatId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoChat tipo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public int getChatId() { return chatId; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public TipoChat getTipo() { return tipo; }
    public void setTipo(TipoChat tipo) { this.tipo = tipo; }
    
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
