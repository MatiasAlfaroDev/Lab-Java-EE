package com.chat.model;

import java.time.LocalDateTime;
import java.util.List;
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

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)    
    @Column(nullable = false, length = 20)
    private TipoEstado estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false, length = 100)
    private String rol;

    @Column(nullable = true)
    private String mfa_secret; 
    
    @Column(name = "push_token", length = 255)
    private String pushToken;

    @Column(name = "public_key", columnDefinition = "text")
    private String publicKey;

    @Column(nullable = false)
    private boolean bloqueado = false;

    @OneToMany(mappedBy = "usuario")
    private List<MiembroChat> chatsIntegrados;

    @PrePersist
    protected void onCreate() { this.fechaCreacion = LocalDateTime.now(); }

    public int getId() { return id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public TipoEstado getEstado() { return estado; }
    public void setEstado(TipoEstado estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getPushToken() {return pushToken;}
    public void setPushToken(String pushToken) {this.pushToken = pushToken;}

    public List<MiembroChat> getChatsIntegrados() {return chatsIntegrados;}
    public void setChatsIntegrados(List<MiembroChat> chatsIntegrados) {this.chatsIntegrados = chatsIntegrados;}

    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado;}

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

}
