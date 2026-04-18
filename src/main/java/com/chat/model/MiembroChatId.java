package com.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MiembroChatId implements Serializable {

    @Column(name = "usuario_id")
    private int usuarioId;

    @Column(name = "chat_id")
    private int chatId;

    public MiembroChatId() {}

    public MiembroChatId(int usuarioId, int chatId) {
        this.usuarioId = usuarioId;
        this.chatId = chatId;
    }

    public int getUsuarioId() { return usuarioId; }
    public int getChatId() { return chatId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MiembroChatId)) return false;
        MiembroChatId that = (MiembroChatId) o;
        return usuarioId == that.usuarioId && chatId == that.chatId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, chatId);
    }
}
