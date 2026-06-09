package com.chat.datatype;

public class ReaccionWsEvent {

    private String type;
    private int mensajeId;
    private int usuarioId;
    private String usuarioNombre;
    private String emoji;

    public ReaccionWsEvent() {}

    public ReaccionWsEvent(
        String type,
        int mensajeId,
        int usuarioId,
        String usuarioNombre,
        String emoji
    ) {
        this.type = type;
        this.mensajeId = mensajeId;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.emoji = emoji;
    }

    public String getType() {
        return type;
    }

    public int getMensajeId() {
        return mensajeId;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public String getEmoji() {
        return emoji;
    }
}