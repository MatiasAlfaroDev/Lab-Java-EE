package com.chat.model;

import com.chat.enums.ChatRol;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "miembro_chat")
public class MiembroChat {

    @EmbeddedId
    private MiembroChatId id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_rol", nullable = false, length = 20)
    private ChatRol chatRol;

    @Column(name = "fecha_unido", nullable = false, updatable = false)
    private LocalDateTime fechaUnido;

    @Column(name = "ultimo_leido")
    private LocalDateTime ultimoLeido;

    @PrePersist
    protected void onCreate() {
        this.fechaUnido = LocalDateTime.now();
    }

    public MiembroChat() {}

    public MiembroChatId getId() { return id; }
    public void setId(MiembroChatId id) { this.id = id; }
    
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    
    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }
    
    public ChatRol getChatRol() { return chatRol; }
    public void setChatRol(ChatRol chatRol) { this.chatRol = chatRol; }
    
    public LocalDateTime getFechaUnido() { return fechaUnido; }

    public LocalDateTime getUltimoLeido() { return ultimoLeido; }
    public void setUltimoLeido(LocalDateTime ultimoLeido) { this.ultimoLeido = ultimoLeido; }
}