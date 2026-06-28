package com.chat.datatype;

import java.util.List;

public class MensajeResponse {

    public int id;
    public int chatId;
    public int sender_id;
    public String sender_username;
    public String sender_initials;
    public String contenido;
    public String tipo;
    public String sent_at;
    public String estado;
    public boolean entregado;
    public boolean leido;
    public boolean editado;
    public boolean eliminado;
    public List<ReaccionDTO> reacciones;
    public int mensajeOrigenId;
    public UploadAdjuntoResponse adjunto;
}