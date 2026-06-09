package com.chat.dao;

import com.chat.model.ReaccionMensaje;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ReaccionDAO {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void guardar(ReaccionMensaje reaccion) {
        em.persist(reaccion);
    }

    @Transactional
    public void eliminar(ReaccionMensaje reaccion) {
        em.remove(em.contains(reaccion)
                ? reaccion
                : em.merge(reaccion));
    }

    @Transactional
    public void actualizar(ReaccionMensaje reaccion) {
        em.merge(reaccion);
    }

    public ReaccionMensaje buscarPorMensajeYUsuario(
            int mensajeId,
            int usuarioId
    ) {

        try {

            return em.createQuery("""
                SELECT r
                FROM ReaccionMensaje r
                WHERE r.mensaje.id = :mensajeId
                AND r.usuarioReaccion.id = :usuarioId
            """, ReaccionMensaje.class)
            .setParameter("mensajeId", mensajeId)
            .setParameter("usuarioId", usuarioId)
            .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }
}