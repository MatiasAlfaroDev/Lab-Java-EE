package com.chat.service;

import com.chat.dao.UsuarioDAO;
import com.chat.model.Usuario;
import com.chat.enums.TipoEstado;
import org.mindrot.jbcrypt.BCrypt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UsuarioService {

    @Inject
    private UsuarioDAO usuarioDAO;

    public void registrarUsuario(String nombre, String email, String password, String rol) {

        // 1. Validar datos
        if (nombre == null || email == null || password == null || rol == null ||
            nombre.isBlank() || email.isBlank() || password.isBlank() || rol.isBlank()) {
            throw new IllegalArgumentException("Datos inválidos");
        }

        // 2. Verificar email existente
        if (usuarioDAO.existeEmail(email)) {
            throw new IllegalArgumentException("El email ya está en uso");
        }

        // 3. Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setRol(rol);

        // 4. Estado inicial
        usuario.setEstado(TipoEstado.OFFLINE);

        // 5. Encriptar contraseña
        String passEncriptada = BCrypt.hashpw(password, BCrypt.gensalt());
        usuario.setPassword(passEncriptada);

        // 6. Guardar en BD
        usuarioDAO.guardar(usuario);
    }

    public Usuario login(String email, String password) {

        // 1. Buscar usuario
        Usuario usuario = usuarioDAO.buscarEmail(email);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no existe");
        }

        // 2. Verificar contraseña (BCrypt)
        if (!BCrypt.checkpw(password, usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        // 3. Cambiar estado
        usuario.setEstado(TipoEstado.ONLINE);

        // 4. Guardar cambio
        usuarioDAO.actualizar(usuario);

        return usuario;
}
}