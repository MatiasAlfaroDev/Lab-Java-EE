package com.chat.model;

import jakarta.persistence.*;

@Entity
@Table(name = "clave_grupo")
public class ClaveGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "chat_id", nullable = false)
    private int chatId;

    @Column(name = "miembro_id", nullable = false)
    private int miembroId;

    @Column(name = "distribuidor_id", nullable = false)
    private int distribuidorId;

    @Column(name = "clave_envuelta", nullable = false, columnDefinition = "text")
    private String claveEnvuelta;

    @Column(nullable = false)
    private long version;

    public int getId() { return id; }
    public int getChatId() { return chatId; }
    public void setChatId(int chatId) { this.chatId = chatId; }
    public int getMiembroId() { return miembroId; }
    public void setMiembroId(int miembroId) { this.miembroId = miembroId; }
    public int getDistribuidorId() { return distribuidorId; }
    public void setDistribuidorId(int distribuidorId) { this.distribuidorId = distribuidorId; }
    public String getClaveEnvuelta() { return claveEnvuelta; }
    public void setClaveEnvuelta(String claveEnvuelta) { this.claveEnvuelta = claveEnvuelta; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
