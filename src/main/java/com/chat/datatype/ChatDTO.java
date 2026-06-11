package com.chat.datatype;

public class ChatDTO {

    private int id;
    private String nombre;

    private String lastMsg;
    private String lastMsgTime;
    private String estado;
    private int unread;

    public ChatDTO(
        int id,
        String nombre,
        String lastMsg,
        String lastMsgTime,
        String estado,
        int unread
    ) {

        this.id = id;
        this.nombre = nombre;
        this.lastMsg = lastMsg;
        this.lastMsgTime = lastMsgTime;
        this.estado = estado;
        this.unread = unread;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEstado() {
        return estado;
    }

    public String getLastMsg() {
        return lastMsg;
    }

    public String getLastMsgTime() {
        return lastMsgTime;
    }

    public int getUnread() {
        return unread;
    }
}

