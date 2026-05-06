package com.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;  
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "voto_encuesta")
public class VotoEncuesta {
    
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name = "voto_id")
    private int votoId; 

    @ManyToOne
    @JoinColumn(name = "opcion_id") 
    private OpcionEncuesta opcion;  

    @ManyToOne
    @JoinColumn(name = "usuario_id")        
    private Usuario usuario;

    @Column(name = "fecha_voto", nullable = false)
    private LocalDateTime fechaVoto; 
    
    @PrePersist
    protected void onCreate() {
        this.fechaVoto = LocalDateTime.now();
    }
    
    public int getVotoId() { return votoId; }
    public OpcionEncuesta getOpcion() { return opcion; }
    public void setOpcion(OpcionEncuesta opcion) { this.opcion = opcion; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFechaVoto() { return fechaVoto; }
    public void setFechaVoto(LocalDateTime fechaVoto) { this.fechaVoto = fechaVoto; }

}
