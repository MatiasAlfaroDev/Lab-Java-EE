package com.chat.datatype;

import java.util.List;

public class CrearEncuestaRequest {

    private String pregunta;
    private List<String> opciones;
    private boolean anonima;
    private String expires_at;

    public String getPregunta() { return pregunta; }
    public void setPregunta(String pregunta) { this.pregunta = pregunta; }
    public List<String> getOpciones() { return opciones; }
    public void setOpciones(List<String> opciones) { this.opciones = opciones; }
    public boolean isAnonima() { return anonima; }
    public void setAnonima(boolean anonima) { this.anonima = anonima; }
    public String getExpires_at() { return expires_at; }
    public void setExpires_at(String expires_at) { this.expires_at = expires_at; }
}
