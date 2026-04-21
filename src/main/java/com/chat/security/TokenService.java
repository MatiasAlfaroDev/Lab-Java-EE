package com.chat.security;

import com.chat.model.Usuario;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TokenService {

    // generar token simple (después se puede mejorar a JWT)
    public String generarToken(Usuario user) {
        return user.getId() + "-token";
    }

    // validar token y obtener userId
    public Long validarToken(String token) {
        try {
            String id = token.split("-")[0];
            return Long.parseLong(id);
        } catch (Exception e) {
            throw new RuntimeException("Token inválido");
        }
    }
}
