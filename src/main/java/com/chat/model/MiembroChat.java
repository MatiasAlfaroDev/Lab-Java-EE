package com.chat.model;

import com.chat.datatype.DtFecha;
import com.chat.enums.ChatRol;
import jakarta.persistence.*;

@Entity
@Table(name = "miembro_chat")
public class MiembroChat {

    @EmbeddedId
    private MiembroChatId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_rol", nullable = false, length = 20)
    private ChatRol chatRol;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "dia",  column = @Column(name = "fecha_unido_dia")),
        @AttributeOverride(name = "mes",  column = @Column(name = "fecha_unido_mes")),
        @AttributeOverride(name = "anio", column = @Column(name = "fecha_unido_anio"))
    })
    private DtFecha fechaUnido;

    public MiembroChatId getId() { return id; }
    public void setId(MiembroChatId id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }
    public ChatRol getChatRol() { return chatRol; }
    public void setChatRol(ChatRol chatRol) { this.chatRol = chatRol; }
    public DtFecha getFechaUnido() { return fechaUnido; }
    public void setFechaUnido(DtFecha fechaUnido) { this.fechaUnido = fechaUnido; }
}
