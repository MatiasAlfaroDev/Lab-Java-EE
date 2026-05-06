package com.chat.model;

import java.time.LocalDateTime;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "adjunto")
public class Adjunto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int adjuntoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mensaje_referencia_id")
    private Mensaje mensajeReferencia;

    @Column(nullable = false, length = 255)
    private String nombreArchivo;

    @Column(nullable = false, length = 255)
    private String urlArchivo;

    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;          

    @Column(nullable = false)       
    private Long tamanoArchivo;

    @PrePersist
    protected void onCreate() {
        this.fechaSubida = LocalDateTime.now();
    }       

    public int getAdjuntoId() { return adjuntoId; }     
    public Mensaje getMensajeReferencia() { return mensajeReferencia; }
    public void setMensajeReferencia(Mensaje mensajeReferencia) { this.mensajeReferencia = mensajeReferencia; }
    public String getNombreArchivo() { return nombreArchivo; }      
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public String getUrlArchivo() { return urlArchivo; }
    public void setUrlArchivo(String urlArchivo) { this.urlArchivo = urlArchivo; }
    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public Long getTamanoArchivo() { return tamanoArchivo; }
    public void setTamanoArchivo(Long tamanoArchivo) { this.tamanoArchivo = tamanoArchivo; }

}
