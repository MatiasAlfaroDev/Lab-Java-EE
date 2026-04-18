package com.chat.model;

import com.chat.datatype.DtFecha;
import com.chat.enums.TipoEstado;
import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEstado estado;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "dia",  column = @Column(name = "fecha_creacion_dia")),
        @AttributeOverride(name = "mes",  column = @Column(name = "fecha_creacion_mes")),
        @AttributeOverride(name = "anio", column = @Column(name = "fecha_creacion_anio"))
    })
    private DtFecha fechaCreacion;

    @Column(nullable = false, length = 100)
    private String rol;

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public TipoEstado getEstado() { return estado; }
    public void setEstado(TipoEstado estado) { this.estado = estado; }
    public DtFecha getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(DtFecha fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
