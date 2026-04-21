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
    public void crearChat(String nombre, String tipo, List<Integer> usuariosIds) {

        // 1. Validaciones básicas
        if (nombre == null || tipo == null || usuariosIds == null || usuariosIds.isEmpty()) {
            throw new IllegalArgumentException("Datos inválidos");
        }

        // 2. Crear chat
        Chat chat = new Chat();
        chat.setNombre(nombre);
        chat.setTipo(TipoChat.valueOf(tipo));

        chatDAO.guardar(chat);

        // IMPORTANTE: para tener el chatId
        em.flush();

        // 3. Crear miembros
        for (int i = 0; i < usuariosIds.size(); i++) {

            Integer userId = usuariosIds.get(i);

            Usuario usuario = usuarioDAO.buscarPorId(userId);

            if (usuario == null) {
                throw new RuntimeException("Usuario no existe: " + userId);
            }

            MiembroChat miembro = new MiembroChat();
            miembro.setChat(chat);
            miembro.setUsuario(usuario);

            // ROL
            if (i == 0) {
                miembro.setChatRol(ChatRol.CREADOR);
            } else {
                miembro.setChatRol(ChatRol.MIEMBRO);
            }

            // ID compuesto
            MiembroChatId id = new MiembroChatId(userId, chat.getChatId());
            miembro.setId(id);

            em.persist(miembro);
        }
    }

    public List<Chat> obtenerChats() {
        return chatDAO.obtenerTodosChats();
    }
}