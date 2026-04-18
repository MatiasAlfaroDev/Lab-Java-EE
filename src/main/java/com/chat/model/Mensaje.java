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

    @Column(name = "fecha_enviado", nullable = false, updatable = false)
    private LocalDateTime fechaEnviado;
  
    @Column(nullable = false)
    private boolean editado;

    @PrePersist
    protected void onCreate() {
        this.fechaEnviado = LocalDateTime.now();
        this.editado = false;
    }


    public int getId() { return id; }

    public Usuario getEmisor() { return emisor; }
    public void setEmisor(Usuario emisor) { this.emisor = emisor; }
    
    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }
    
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    
    public DtFecha getFechaEnviado() { return fechaEnviado; }
   
    public boolean isEditado() { return editado; }
    public void setEditado(boolean editado) { this.editado = editado; }
}
