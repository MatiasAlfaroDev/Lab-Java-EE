package com.chat.dao;

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
    return em.createQuery("SELECT c FROM Chat c", Chat.class)
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
}