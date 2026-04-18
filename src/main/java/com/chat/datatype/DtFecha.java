package com.chat.datatype;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class DtFecha implements Serializable {

    private int dia;
    private int mes;
    private int anio;

    public DtFecha() {}

    public DtFecha(int dia, int mes, int anio) {
        this.dia = dia;
        this.mes = mes;
        this.anio = anio;
    }

    public int getDia() { return dia; }
    public void setDia(int dia) { this.dia = dia; }
    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }
    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }
}
