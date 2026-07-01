package com.chat.datatype;

public class UsuarioAdminDTO {

    private int id;
    private String nombre;
    private String email;
    private String rol;
    private String departamento;
    private String status;

    public UsuarioAdminDTO(int id, String nombre, String email, String rol, String departamento, String status) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.departamento = departamento;
        this.status = status;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getRol() { return rol; }
    public String getDepartamento() { return departamento; }
    public String getStatus() { return status; }
}
