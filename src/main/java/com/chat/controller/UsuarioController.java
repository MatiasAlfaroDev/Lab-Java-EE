package com.chat.controller;

import com.chat.service.UsuarioService;

public class UsuarioController {
    
    private UsuarioService usuarioService = new UsuarioService();

    public void registrarUsuario(String nombre, String email, String password, String rol) {
    usuarioService.registrarUsuario(nombre, email, password, rol);
    }
}
