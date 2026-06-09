package com.chat.datatype;

public class ReaccionRequest {

    private int mensajeId;
    private String emoji;

    public ReaccionRequest() {}
    public int getMensajeId() {
        return mensajeId;
    }
    public String getEmoji() {
        return emoji;
    }
    public void setMensajeId(int mensajeId) {
        this.mensajeId = mensajeId;
    }
    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}