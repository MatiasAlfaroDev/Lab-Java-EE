package com.chat.service;

import com.chat.dao.ChatDAO;
import com.chat.dao.UsuarioDAO;
import com.chat.model.*;
import com.chat.enums.ChatRol;
import com.chat.enums.TipoChat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@ApplicationScoped
public class ChatService {

    @Inject
    private ChatDAO chatDAO;

    @Inject
    private UsuarioDAO usuarioDAO;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void crearChat(String nombre, String tipo, List<Integer> usuariosIds, Long userId) {

        // 1. Validaciones básicas
        if (nombre == null || tipo == null || usuariosIds == null || usuariosIds.isEmpty()) {
            throw new IllegalArgumentException("Datos inválidos");
        }

        // 2. Validar tipo de chat
        TipoChat tipoChat;
        try {
            tipoChat = TipoChat.valueOf(tipo);
        } catch (Exception e) {
            throw new IllegalArgumentException("Tipo de chat inválido");
        }

        // 3. Validar que el creador esté en la lista
        if (!usuariosIds.contains(userId.intValue())) {
            throw new RuntimeException("El creador debe estar en la lista de usuarios");
        }

        // 4. Crear chat
        Chat chat = new Chat();
        chat.setNombre(nombre);
        chat.setTipo(TipoChat.valueOf(tipo));

        chatDAO.guardar(chat);

        // IMPORTANTE: para tener el chatId
        em.flush();

        // 5. Crear miembros
        for (Integer userIdLista : usuariosIds) {

            Usuario usuario = usuarioDAO.buscarPorId(userIdLista);

            if (usuario == null) {
                throw new RuntimeException("Usuario no existe: " + userIdLista);
            }

            MiembroChat miembro = new MiembroChat();
            miembro.setChat(chat);
            miembro.setUsuario(usuario);

            // Definir rol correctamente
            if (userIdLista.equals(userId.intValue())) {
                miembro.setChatRol(ChatRol.CREADOR);
            } else {
                miembro.setChatRol(ChatRol.MIEMBRO);
            }

            // ID compuesto
            MiembroChatId id = new MiembroChatId(userIdLista, chat.getChatId());
            miembro.setId(id);

            em.persist(miembro);
        }
    }

    public List<Chat> obtenerChats() {
        return chatDAO.obtenerTodosChats();
    }

     @Transactional
    public void agregarMiembro(int chatId, int adminId, int usuarioAgregarId) {

        // 1. Obtener chat
        Chat chat = chatDAO.buscarPorId(chatId);
        if (chat == null) {
            throw new RuntimeException("El chat no existe");
        }

        // 2. Validar tipo (solo grupal)
        if (chat.getTipo() != TipoChat.GRUPO) {
            throw new RuntimeException("Solo se pueden agregar miembros a chats grupales");
        }

        // 3. Verificar que el admin pertenece al chat
        MiembroChat admin = chatDAO.buscarMiembro(chatId, adminId);
        if (admin == null) {
            throw new RuntimeException("No perteneces al chat");
        }

        // 4. Verificar permisos
        if (admin.getChatRol() != ChatRol.ADMINISTRADOR && admin.getChatRol() != ChatRol.CREADOR) {
            throw new RuntimeException("No tienes permisos");
        }

        // 5. Verificar usuario a agregar
        Usuario usuario = usuarioDAO.buscarPorId(usuarioAgregarId);
        if (usuario == null) {
            throw new RuntimeException("Usuario no existe");
        }

        // 6. Verificar que no esté ya en el chat
        MiembroChat existente = chatDAO.buscarMiembro(chatId, usuarioAgregarId);
        if (existente != null) {
            throw new RuntimeException("El usuario ya está en el chat");
        }

        // 7. Crear miembro
        MiembroChat miembro = new MiembroChat();
        miembro.setChat(chat);
        miembro.setUsuario(usuario);
        miembro.setChatRol(ChatRol.MIEMBRO);

        MiembroChatId id = new MiembroChatId(usuarioAgregarId, chatId);
        miembro.setId(id);

        // 8. Guardar
        em.persist(miembro);
    }

    @Transactional
    public void eliminarMiembro(int chatId, int adminId, int usuarioEliminarId) {

        Chat chat = chatDAO.buscarPorId(chatId);
        if (chat == null) {
            throw new RuntimeException("El chat no existe");
        }

        // solo grupales
        if (chat.getTipo() != TipoChat.GRUPO) {
            throw new RuntimeException("Solo se pueden eliminar miembros en chats grupales");
        }

        // validar admin
        MiembroChat admin = chatDAO.buscarMiembro(chatId, adminId);
        if (admin == null) {
            throw new RuntimeException("No perteneces al chat");
        }

        if (admin.getChatRol() != ChatRol.ADMINISTRADOR &&
            admin.getChatRol() != ChatRol.CREADOR) {
            throw new RuntimeException("No tienes permisos");
        }

        // validar que exista el miembro
        MiembroChat miembro = chatDAO.buscarMiembro(chatId, usuarioEliminarId);
        if (miembro == null) {
            throw new RuntimeException("El usuario no pertenece al chat");
        }

        if (miembro.getChatRol() == ChatRol.CREADOR) {
        List<MiembroChat> miembros = chatDAO.obtenerMiembros(chatId);

        boolean hayOtroAdmin = miembros.stream().anyMatch(m ->
            m.getChatRol() == ChatRol.ADMINISTRADOR &&
            m.getUsuario().getId() != usuarioEliminarId
        );

        if (!hayOtroAdmin) {
            throw new RuntimeException("No se puede eliminar al creador sin otro administrador");
        }
    }
        // eliminar
        chatDAO.eliminarMiembro(chatId, usuarioEliminarId);
    }


}