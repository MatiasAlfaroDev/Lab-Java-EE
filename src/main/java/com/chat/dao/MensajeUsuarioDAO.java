package com.chat.dao;

import com.chat.model.MensajeUsuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;

@ApplicationScoped
public class MensajeUsuarioDAO {
    
    @PersistenceContext
    private EntityManager em;

    // GUARDAR
    public void guardar(MensajeUsuario mu) {
        em.persist(mu);
    }

    // BUSCAR por mensaje + usuario (para ticks)
    public MensajeUsuario buscarPorMensajeYUsuario(int mensajeId, int usuarioId) {

        TypedQuery<MensajeUsuario> query = em.createQuery(
            "SELECT mu FROM MensajeUsuario mu " +
            "WHERE mu.mensaje.id = :mensajeId " +
            "AND mu.receptor.id = :usuarioId",
            MensajeUsuario.class
        );

        query.setParameter("mensajeId", mensajeId);
        query.setParameter("usuarioId", usuarioId);

        List<MensajeUsuario> result = query.getResultList();

        return result.isEmpty() ? null : result.get(0);
    }

    // MENSAJES NO LEÍDOS (para marcar leído al entrar al chat)
    public List<MensajeUsuario> findNoLeidos(int chatId, int usuarioId) {

        return em.createQuery(
            "SELECT mu FROM MensajeUsuario mu " +
            "WHERE mu.mensaje.chat.chatId = :chatId " +
            "AND mu.receptor.id = :usuarioId " +
            "AND mu.fechaLeido IS NULL",
            MensajeUsuario.class
        )
        .setParameter("chatId", chatId)
        .setParameter("usuarioId", usuarioId)
        .getResultList();
    }

    // MARCAR ENTREGADO (opcional si lo usás desde DAO)
    public void marcarEntregado(int mensajeId, int usuarioId) {

        MensajeUsuario mu = buscarPorMensajeYUsuario(mensajeId, usuarioId);

        if (mu != null && mu.getFechaEntregado() == null) {
            mu.setFechaEntregado(java.time.LocalDateTime.now());
        }
    }

    public List<MensajeUsuario> listarPorMensaje(int mensajeId) {

        return em.createQuery("""
            SELECT mu
            FROM MensajeUsuario mu
            WHERE mu.mensaje.id = :mensajeId
        """, MensajeUsuario.class)
        .setParameter("mensajeId", mensajeId)
        .getResultList();   
    }

    public void update(MensajeUsuario mu) {
        em.merge(mu);
    }
}