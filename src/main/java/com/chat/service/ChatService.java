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
}