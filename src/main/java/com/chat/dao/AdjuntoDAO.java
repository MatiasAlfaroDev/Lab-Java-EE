package com.chat.dao;

import com.chat.model.Adjunto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class AdjuntoDAO {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void guardar(Adjunto adjunto) {
        em.persist(adjunto);
    }

    public Adjunto buscarPorId(int id) {
        return em.find(Adjunto.class, id);
    }

    public List<Adjunto> listarPorMensaje(int mensajeId) {
        return em.createQuery(
                "SELECT a FROM Adjunto a WHERE a.mensajeReferencia.id = :mensajeId",
                Adjunto.class)
                .setParameter("mensajeId", mensajeId)
                .getResultList();
    }

    @Transactional
    public void actualizar(Adjunto adjunto) {
        em.merge(adjunto);
    }

    @Transactional
    public void eliminar(Adjunto adjunto) {
        em.remove(em.contains(adjunto) ? adjunto : em.merge(adjunto));
    }
}