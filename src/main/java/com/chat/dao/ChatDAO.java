package com.chat.dao;

import com.chat.model.Chat;
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
}