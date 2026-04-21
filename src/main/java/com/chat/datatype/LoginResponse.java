package com.chat.datatype;

public class LoginResponse {

    private String token;
    private UsuarioDTO usuario;

    public LoginResponse(String token, UsuarioDTO usuario) {
        this.token = token;
        this.usuario = usuario;
    }

    public String getToken() {
        return token;
    }

    public UsuarioDTO getUsuario() {
        return usuario;
    }
}
