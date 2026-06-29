package com.chat.datatype;

import java.util.List;

public class ClaveGrupoRequest {

    public static class Envuelta {
        public int miembroId;
        public String claveEnvuelta;
    }

    public long version;
    public int distribuidorId;
    public List<Envuelta> envueltas;
}
