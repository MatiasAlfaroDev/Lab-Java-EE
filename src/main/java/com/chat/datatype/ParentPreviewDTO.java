package com.chat.datatype;

public class ParentPreviewDTO {
    public String sender_username;
    public String contenido;
    public String tipo;
    public String nombreArchivo;

    public ParentPreviewDTO(String sender_username, String contenido, String tipo, String nombreArchivo) {
        this.sender_username = sender_username;
        this.contenido = contenido;
        this.tipo = tipo;
        this.nombreArchivo = nombreArchivo;
    }
}
