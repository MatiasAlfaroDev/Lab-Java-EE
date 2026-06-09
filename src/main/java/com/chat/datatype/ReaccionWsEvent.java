package com.chat.datatype;

public class ReaccionWsEvent {

    private String type;
    private int mensajeId;
    private int usuarioId;
    private String emoji;

    public ReaccionWsEvent() {}

    public ReaccionWsEvent(
        String type,
        int mensajeId,
        int usuarioId,
        String emoji
    ) {
        this.type = type;
        this.mensajeId = mensajeId;
        this.usuarioId = usuarioId;
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

    public String getEmoji() {
        return emoji;
    }
}