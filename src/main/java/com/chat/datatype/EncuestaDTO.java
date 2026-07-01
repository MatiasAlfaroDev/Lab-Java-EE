package com.chat.datatype;

import java.util.List;

public class EncuestaDTO {

    public static class OpcionDTO {
        public int id;
        public String texto;
        public long votos;

        public OpcionDTO(int id, String texto, long votos) {
            this.id = id;
            this.texto = texto;
            this.votos = votos;
        }
    }

    public int id;
    public String pregunta;
    public List<OpcionDTO> opciones;
    public long total;
    public Integer myVote;
    public boolean anonima;
    public String expires_at;
}
