package com.chat.datatype;

public class UsuarioDTO {

    private int id;
    private String nombre;
    private String email;
    private String rol;
    private String estado;
    private boolean bloqueado;
    private String fotoPerfilUrl;

    public UsuarioDTO() {}

    public UsuarioDTO(int id, String nombre, String email, String rol, String estado, boolean bloqueado, String fotoPerfilUrl) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.estado = estado;
        this.bloqueado = bloqueado;
        this.fotoPerfilUrl = fotoPerfilUrl;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getRol() { return rol; }
    public String getEstado() { return estado; }
    public boolean isBloqueado() { return bloqueado; }
    public String getFotoPerfilUrl() { return fotoPerfilUrl; }

}
