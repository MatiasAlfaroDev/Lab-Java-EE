package com.chat.security;

import com.chat.model.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class TokenService {

    private Map<String, Long> tokens = new HashMap<>();

    public String generarToken(Usuario user) {
        String token = user.getId() + "-token";
        tokens.put(token, (long) user.getId());
        return token;
    }

    public Long validarToken(String token) {

        if (token == null || token.isBlank()) {
            throw new RuntimeException("Token inválido");
        }

        token = token.replace("Bearer ", "").trim();
         

        Long userId = tokens.get(token);

        if (userId == null) {
            throw new RuntimeException("Token inválido");
        }

        return userId;
    }

    public void eliminarToken(String token) {
        if (token != null) {
            tokens.remove(token.trim());
        }
    }
}
