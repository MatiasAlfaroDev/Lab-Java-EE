package com.chat.service;

import com.chat.dao.AdjuntoDAO;
import com.chat.dao.ClaveGrupoDAO;
import com.chat.dao.UsuarioDAO;
import com.chat.datatype.UsuarioAdminDTO;
import com.chat.datatype.UsuarioDTO;
import com.chat.model.Adjunto;
import com.chat.model.Usuario;
import com.chat.enums.TipoEstado;
import com.chat.websocket.ChatWebSocket;
import org.mindrot.jbcrypt.BCrypt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class UsuarioService {

    @Inject
    private UsuarioDAO usuarioDAO;

    @Inject
    private ClaveGrupoDAO claveGrupoDAO;

    @Inject
    private AdjuntoDAO adjuntoDAO;

    public void registrarUsuario(String nombre, String email, String password, String rol) {

        // 1. Validar datos
        if (nombre == null || email == null || password == null || rol == null ||
            nombre.isBlank() || email.isBlank() || password.isBlank() || rol.isBlank()) {
            throw new IllegalArgumentException("Datos inválidos");
        }

        if (
            !password.matches(
                "^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$"
            )
        ) {
            throw new IllegalArgumentException(
                "Contraseña inválida"
            );
        }

        email = email.trim().toLowerCase();

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

         if (
            email == null ||
            password == null ||
            email.isBlank() ||
            password.isBlank()
        ) {
            throw new IllegalArgumentException("Datos inválidos");
        }

        email = email.trim().toLowerCase();

        // 1. Buscar usuario
        Usuario usuario = usuarioDAO.buscarEmail(email);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no existe");
        }

        // 2. Verificar contraseña (BCrypt)
        if (!BCrypt.checkpw(password, usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        if (usuario.isBloqueado()) {
            throw new IllegalArgumentException("Usuario bloqueado");
        }

        // 3. Cambiar estado
        usuario.setEstado(TipoEstado.ONLINE);

        // 4. Guardar cambio
        usuarioDAO.actualizar(usuario);

        return usuario;
    }

    public void logout(int userId) {

        Usuario usuario =
            usuarioDAO.buscarPorId(userId);

        if (usuario != null) {

            usuario.setEstado(TipoEstado.OFFLINE);

            usuario.setPushToken(null);

            usuarioDAO.actualizar(usuario);
        }
    }

    public List<UsuarioDTO> listarUsuarios() {
    return usuarioDAO.listar()
            .stream()
            .map(u -> new UsuarioDTO(
                    u.getId(),
                    u.getNombre(),
                    u.getEmail(),
                    u.getRol(),
                    u.getEstado().name(),
                    u.isBloqueado(),
                    u.getFotoPerfil() != null ? u.getFotoPerfil().getUrlArchivo() : null
            ))
            .toList();
    }

    public void guardarPushToken(
        int usuarioId,
        String pushToken
    ) {

        Usuario usuario =
            usuarioDAO.buscarPorId(usuarioId);

        if (usuario == null) {
            throw new IllegalArgumentException(
                "Usuario no existe"
            );
        }

        usuarioDAO.limpiarToken(pushToken);
        usuario.setPushToken(pushToken);

        usuarioDAO.actualizar(usuario);
    }

    public void bloquearUsuario(int idAdmin, int idUsuario) {
        Usuario admin = usuarioDAO.buscarPorId(idAdmin);
        if (!"ADMIN".equalsIgnoreCase(admin.getRol())) {
            throw new IllegalArgumentException("No autorizado");
        }

        Usuario usuario = usuarioDAO.buscarPorId(idUsuario);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        usuario.setBloqueado(true);
        usuarioDAO.actualizar(usuario);
    }
    public void desbloquearUsuario(int idAdmin, int idUsuario) {
        Usuario admin = usuarioDAO.buscarPorId(idAdmin);
        if (!"ADMIN".equalsIgnoreCase(admin.getRol())) {
            throw new IllegalArgumentException("No autorizado");
        }

        Usuario usuario = usuarioDAO.buscarPorId(idUsuario);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        usuario.setBloqueado(false);
        usuarioDAO.actualizar(usuario);
    }

    public List<UsuarioAdminDTO> listarUsuariosAdmin(int adminId) {
        Usuario admin = usuarioDAO.buscarPorId(adminId);
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRol())) {
            throw new IllegalArgumentException("No autorizado");
        }

        return usuarioDAO.listarTodos().stream()
            .map(u -> new UsuarioAdminDTO(
                u.getId(),
                u.getNombre(),
                u.getEmail(),
                u.getRol(),
                null,
                u.isBloqueado() ? "SUSPENDED" : "ACTIVE"
            ))
            .toList();
    }

    public void cambiarEstadoUsuario(int adminId, int usuarioId, String status) {
        if (!"ACTIVE".equalsIgnoreCase(status) && !"SUSPENDED".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Status inválido");
        }

        if ("SUSPENDED".equalsIgnoreCase(status)) {
            bloquearUsuario(adminId, usuarioId);
        } else {
            desbloquearUsuario(adminId, usuarioId);
        }
    }

    public void guardarPublicKey(int usuarioId, String publicKey) {
        if (publicKey == null || publicKey.isBlank())
            throw new IllegalArgumentException("Clave inválida");

        String anterior = usuarioDAO.buscarPublicKey(usuarioId);
        usuarioDAO.guardarPublicKey(usuarioId, publicKey);

        // La clave rotó (no es el primer registro): las claves de grupo que otros
        // miembros envolvieron con la clave anterior ya no se pueden desenvolver.
        // Se invalidan y se avisa para que algún cliente conectado las redistribuya.
        if (anterior != null && !anterior.isBlank() && !anterior.equals(publicKey)) {
            claveGrupoDAO.eliminarPorMiembro(usuarioId);

            String json = String.format(
                "{\"type\":\"PUBLIC_KEY_ROTATED\",\"usuarioId\":%d}",
                usuarioId
            );
            ChatWebSocket.broadcastAll(json);
        }
    }

    public String obtenerPublicKey(int usuarioId) {
        return usuarioDAO.buscarPublicKey(usuarioId);
    }

    public void actualizarFotoPerfil(int usuarioId, String urlArchivo, String nombreArchivo, Long tamanoArchivo, String mimeType) {
        if (urlArchivo == null || urlArchivo.isBlank())
            throw new IllegalArgumentException("Foto inválida");

        Usuario usuario = usuarioDAO.buscarPorId(usuarioId);
        if (usuario == null) throw new IllegalArgumentException("Usuario no existe");

        Adjunto adjunto = new Adjunto();
        adjunto.setUrlArchivo(urlArchivo);
        adjunto.setNombreArchivo(nombreArchivo);
        adjunto.setTamanoArchivo(tamanoArchivo);
        adjunto.setMimeType(mimeType);
        adjuntoDAO.guardar(adjunto);

        usuario.setFotoPerfil(adjunto);
        usuarioDAO.actualizar(usuario);
    }

}