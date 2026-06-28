package com.chat.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "mensaje_usuario")
public class MensajeUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_receptor_id", nullable = false)
    private Usuario receptor; // Se mantiene 'emisor' según UML (aunque sea el receptor del estado)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    @Column(name = "fecha_entregado")
    private LocalDateTime fechaEntregado;

    @Column(name = "fecha_leido")
    private LocalDateTime fechaLeido;

    @Column(name = "fecha_eliminado")
    private LocalDateTime fechaEliminado;   

    @Column(nullable = true)
    private boolean eliminado;


    public int getId() {return id;}

    public Usuario getReceptor() {return receptor;}
    public void setReceptor(Usuario receptor) {this.receptor = receptor;}

    public Mensaje getMensaje() {return mensaje;}
    public void setMensaje(Mensaje mensaje) {this.mensaje = mensaje;}

    public LocalDateTime getFechaEntregado() {return fechaEntregado;}
    public void setFechaEntregado(LocalDateTime fechaEntregado) {this.fechaEntregado = fechaEntregado;}

    public LocalDateTime getFechaLeido() {return fechaLeido;}
    public void setFechaLeido(LocalDateTime fechaLeido) {this.fechaLeido = fechaLeido;}

    public boolean isEliminado() {return eliminado;}
    public void setEliminado(boolean eliminado) {this.eliminado = eliminado;}

    public LocalDateTime getFechaEliminado() {return fechaEliminado;}
    public void setFechaEliminado(LocalDateTime fechaEliminado) {this.fechaEliminado = fechaEliminado;}

    
}