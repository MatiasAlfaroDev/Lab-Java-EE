package com.chat.dao;

import com.chat.model.Mensaje;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class MensajeDAO {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void guardar(Mensaje mensaje) {
        em.persist(mensaje);
    }

    public List<Mensaje> listarPorChat(int chatId) {
    return em.createQuery(
            "SELECT m FROM Mensaje m WHERE m.chat.chatId = :chatId ORDER BY m.id ASC",
            Mensaje.class
        )
        .setParameter("chatId", chatId)
        .getResultList();
    }

    public Long contarNoLeidos(int chatId, int usuarioId, java.time.LocalDateTime ultimoLeido) {

        return em.createQuery("""
            SELECT COUNT(m)
            FROM Mensaje m
            WHERE m.chat.chatId = :chatId
            AND m.fechaEnviado > :ultimoLeido
            AND m.emisor.id != :usuarioId
        """, Long.class)
        .setParameter("chatId", chatId)
        .setParameter("ultimoLeido", ultimoLeido)
        .setParameter("usuarioId", usuarioId)
        .getSingleResult();
    }
    
}