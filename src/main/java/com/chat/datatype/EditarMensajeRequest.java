package com.chat.datatype;

public class EditarMensajeRequest {

    private String contenido;
    private boolean cifrado;

    public EditarMensajeRequest() {
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public boolean isCifrado() {
        return cifrado;
    }

    public void setCifrado(boolean cifrado) {
        this.cifrado = cifrado;
    }
}
