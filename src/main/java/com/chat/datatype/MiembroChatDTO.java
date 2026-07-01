package com.chat.datatype;

public class MiembroChatDTO {

    public int id;
    public String nombre;
    public String email;
    public String rol;
    public String fotoPerfilUrl;

    public MiembroChatDTO() {}

    public MiembroChatDTO(int id, String nombre, String email, String rol, String fotoPerfilUrl) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.fotoPerfilUrl = fotoPerfilUrl;
    }
}
