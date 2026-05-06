package com.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


@Entity
@Table(name = "opcion_encuesta")
public class OpcionEncuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name = "opcion_id")
    private int opcionId;

    @ManyToOne
    @JoinColumn(name = "encuesta_id")
    private Encuesta encuesta;

    @Column(name = "texto_opcion")
    private String textoOpcion;
 
    public int getOpcionId() { return opcionId; }
    public Encuesta getEncuesta() { return encuesta; }
    public void setEncuesta(Encuesta encuesta) { this.encuesta = encuesta; }
    public String getTextoOpcion() { return textoOpcion; }
    public void setTextoOpcion(String textoOpcion) { this.textoOpcion = textoOpcion; }
    
}   
