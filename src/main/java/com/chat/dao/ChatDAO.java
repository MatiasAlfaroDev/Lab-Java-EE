package com.chat.dao;

import com.chat.enums.TipoChat;
import com.chat.model.Chat;
import com.chat.model.MiembroChat;
import com.chat.model.MiembroChatId;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ChatDAO {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void guardar(Chat chat) {
        em.persist(chat);
    }

    public List<Chat> obtenerTodosChats() {
        return em.createQuery("""
            SELECT DISTINCT c FROM Chat c
            LEFT JOIN FETCH c.miembros m
            LEFT JOIN FETCH m.usuario
        """, Chat.class).getResultList();
    }

    public List<Chat> obtenerChatsPorUsuario(int userId) {
        return em.createQuery("""
            SELECT DISTINCT c FROM Chat c
            JOIN FETCH c.miembros m
            JOIN FETCH m.usuario
            WHERE m.usuario.id = :userId
        """, Chat.class)
        .setParameter("userId", userId)
        .getResultList();
    }

    public Chat buscarPorId(int chatId) {
    return em.find(Chat.class, chatId);
    }

    public MiembroChat buscarMiembro(int chatId, int usuarioId) {
        MiembroChatId id = new MiembroChatId(usuarioId, chatId);
        return em.find(MiembroChat.class, id);
    }

    @Transactional
    public void guardarMiembro(MiembroChat miembro) {
        em.persist(miembro);
    }

    @Transactional
    public void eliminarMiembro(int chatId, int usuarioId) {
        MiembroChat miembro = em.find(
            MiembroChat.class,
            new MiembroChatId(usuarioId, chatId)
        );

        if (miembro != null) {
            em.remove(miembro);
        }
    }

    public List<MiembroChat> obtenerMiembros(int chatId) {
    return em.createQuery(
        "SELECT m FROM MiembroChat m WHERE m.chat.chatId = :chatId",
        MiembroChat.class
    )
    .setParameter("chatId", chatId)
    .getResultList();
    }

    public Chat buscarChatPrivado(int user1, int user2) {
        List<Chat> chats = em.createQuery("""
            SELECT c FROM Chat c
            JOIN MiembroChat m1 ON m1.chat = c
            JOIN MiembroChat m2 ON m2.chat = c
            WHERE c.tipo = :tipo
            AND (
                (m1.usuario.id = :user1 AND m2.usuario.id = :user2)
                OR
                (m1.usuario.id = :user2 AND m2.usuario.id = :user1)
            )
        """, Chat.class)
        .setParameter("tipo", TipoChat.PRIVADO)
        .setParameter("user1", user1)
        .setParameter("user2", user2)
        .getResultList();

        return chats.isEmpty() ? null : chats.get(0);
    }
}