package com.chat.datatype;

import java.util.List;

public class CrearChatRequest {

    private String nombre;
    private String tipo; // PRIVADO o GRUPAL
    private List<Integer> usuarios;

    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public List<Integer> getUsuarios() { return usuarios; }
}