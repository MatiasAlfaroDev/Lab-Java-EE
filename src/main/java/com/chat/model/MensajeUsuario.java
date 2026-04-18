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
    private Usuario emisor; // Se mantiene 'emisor' según tu UML (aunque sea el receptor del estado)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    @Column(name = "fecha_entregado")
    private LocalDateTime fechaEntregado;

    @Column(name = "fecha_leido")
    private LocalDateTime fechaLeido;

    @Column(nullable = false)
    private boolean eliminado;


    public int getId() {return id;}

    public Usuario getEmisor() {return emisor;}
    public void setEmisor(Usuario emisor) {this.emisor = emisor;}

    public Mensaje getMensaje() {return mensaje;}
    public void setMensaje(Mensaje mensaje) {this.mensaje = mensaje;}

    public LocalDateTime getFechaEntregado() {return fechaEntregado;}
    public void setFechaEntregado(LocalDateTime fechaEntregado) {this.fechaEntregado = fechaEntregado;}

    public LocalDateTime getFechaLeido() {return fechaLeido;}
    public void setFechaLeido(LocalDateTime fechaLeido) {this.fechaLeido = fechaLeido;}

    public boolean isEliminado() {return eliminado;}
    public void setEliminado(boolean eliminado) {this.eliminado = eliminado;}

    
}