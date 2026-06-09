package com.chat.dao;

import com.chat.enums.TipoChat;
import com.chat.model.Chat;
import com.chat.model.Mensaje;
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
            SELECT DISTINCT c
            FROM Chat c
            JOIN FETCH c.miembros
            JOIN FETCH c.miembros.usuario
            WHERE c.chatId IN (

                SELECT mc.chat.chatId
                FROM MiembroChat mc
                WHERE mc.usuario.id = :userId
            )
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

    public Chat buscarChatPrivado(
        int user1,
        int user2
    ) {

        List<Chat> chats = em.createQuery("""
            SELECT c
            FROM Chat c
            JOIN c.miembros m
            WHERE c.tipo = :tipo
            AND m.usuario.id IN (:user1, :user2)
            GROUP BY c
            HAVING COUNT(DISTINCT m.usuario.id) = 2
        """, Chat.class)
        .setParameter("tipo", TipoChat.PRIVADO)
        .setParameter("user1", user1)
        .setParameter("user2", user2)
        .getResultList();

        return chats.isEmpty()
            ? null
            : chats.get(0);
    }

  /*  public String obtenerUltimoMensaje(int chatId) {

        List<String> mensajes = em.createQuery("""
            SELECT m.contenido
            FROM Mensaje m
            WHERE m.chat.chatId = :chatId
            ORDER BY m.fechaEnviado DESC
        """, String.class)
        .setParameter("chatId", chatId)
        .setMaxResults(1)
        .getResultList();

        return mensajes.isEmpty() ? "" : mensajes.get(0);
    } */

    public String obtenerUltimoMensaje(int chatId) {

        List<Mensaje> mensajes = em.createQuery("""
            SELECT m
            FROM Mensaje m
            WHERE m.chat.chatId = :chatId
            ORDER BY m.fechaEnviado DESC
        """, Mensaje.class)
        .setParameter("chatId", chatId)
        .setMaxResults(1)
        .getResultList();

        if (mensajes.isEmpty()) {
            return "";
        }

        Mensaje ultimo = mensajes.get(0);

        return ultimo.isEliminado()
            ? "Mensaje eliminado"
            : ultimo.getContenido();
    }
}