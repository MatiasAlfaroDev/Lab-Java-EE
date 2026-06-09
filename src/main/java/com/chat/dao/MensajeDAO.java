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

    public List<Mensaje> listarPorChat(int chatId, int usuarioId) {
    return em.createQuery(
            "SELECT m FROM Mensaje m WHERE m.chat.chatId = :chatId AND NOT EXISTS (\n" + //
                                "            SELECT 1\n" + //
                                "            FROM MensajeUsuario mu\n" + //
                                "            WHERE mu.mensaje.id = m.id\n" + //
                                "              AND mu.receptor.id = :usuarioId\n" + //
                                "              AND mu.eliminado = true\n" + //
                                "        ) ORDER BY m.fechaEnviado ASC",
            Mensaje.class
        )
        .setParameter("chatId", chatId)
        .setParameter("usuarioId", usuarioId)
        .getResultList();
    }

    public Long contarNoLeidos(
        int chatId,
        int usuarioId
    ) {

        return em.createQuery("""
            SELECT COUNT(mu)
            FROM MensajeUsuario mu
            WHERE mu.mensaje.chat.chatId = :chatId
            AND mu.receptor.id = :usuarioId
            AND mu.mensaje.emisor.id <> :usuarioId
            AND mu.fechaLeido IS NULL
            AND mu.eliminado = false
        """, Long.class)
        .setParameter("chatId", chatId)
        .setParameter("usuarioId", usuarioId)
        .getSingleResult();
    }

    public Mensaje buscarPorId(int mensajeId) {
        return em.find(Mensaje.class, mensajeId);
    }

    public void update(Mensaje mensaje) {
        em.merge(mensaje);
    }

    public List<Integer> findUsuariosByChatId(int chatId) {

        return em.createQuery(
            "SELECT m.usuario.id FROM MiembroChat m WHERE m.chat.id = :chatId",
            Integer.class
        )
        .setParameter("chatId", chatId)
        .getResultList();
    }

}