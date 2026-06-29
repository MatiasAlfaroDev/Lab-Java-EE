package com.chat.datatype;

public class CambiarRolRequest {

    private int chatId;
    private int usuarioId;
    private String rol;

    public int getChatId() { return chatId; }
    public int getUsuarioId() { return usuarioId; }
    public String getRol() { return rol; }
    public void setChatId(int chatId) { this.chatId = chatId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public void setRol(String rol) { this.rol = rol; }
}
