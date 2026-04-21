package com.chat.datatype;

public class AgregarMiembroRequest {

    private int chatId;
    private int usuarioId;

    public int getChatId() { return chatId; }
    public void setChatId(int chatId) { this.chatId = chatId; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
}