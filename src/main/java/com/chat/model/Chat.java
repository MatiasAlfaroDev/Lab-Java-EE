package com.chat.model;

import com.chat.datatype.DtFecha;
import com.chat.enums.TipoChat;
import jakarta.persistence.*;

@Entity
@Table(name = "chat")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int chatId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoChat tipo;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "dia",  column = @Column(name = "fecha_creacion_dia")),
        @AttributeOverride(name = "mes",  column = @Column(name = "fecha_creacion_mes")),
        @AttributeOverride(name = "anio", column = @Column(name = "fecha_creacion_anio"))
    })
    private DtFecha fechaCreacion;

    public int getChatId() { return chatId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoChat getTipo() { return tipo; }
    public void setTipo(TipoChat tipo) { this.tipo = tipo; }
    public DtFecha getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(DtFecha fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
