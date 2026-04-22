package com.chat.datatype;

import java.util.List;

public class CrearChatRequest {

    private String nombre;
    private String tipo; // PRIVADO o GRUPAL
    private List<Integer> usuarios;

    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public List<Integer> getUsuarios() { return usuarios; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTipo(String tipo) { this.tipo = tipo; }  
    public void setUsuarios(List<Integer> usuarios) { this.usuarios = usuarios; }   
}