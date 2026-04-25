package com.chat.datatype;

public class ChatDTO {

    private int id;
    private String nombre;

    public ChatDTO(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
}

