package com.chat.dao;

import com.chat.model.Encuesta;
import com.chat.model.OpcionEncuesta;
import com.chat.model.VotoEncuesta;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class EncuestaDAO {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void guardar(Encuesta encuesta) {
        em.persist(encuesta);
    }

    @Transactional
    public void guardarOpcion(OpcionEncuesta opcion) {
        em.persist(opcion);
    }

    @Transactional
    public void guardarVoto(VotoEncuesta voto) {
        em.persist(voto);
    }

    @Transactional
    public void actualizar(Encuesta encuesta) {
        em.merge(encuesta);
    }

    public Encuesta buscarPorId(int encuestaId) {
        return em.find(Encuesta.class, encuestaId);
    }

    public OpcionEncuesta buscarOpcionPorId(int opcionId) {
        return em.find(OpcionEncuesta.class, opcionId);
    }

    public Encuesta buscarPorMensajeId(int mensajeId) {
        try {
            return em.createQuery(
                "SELECT e FROM Encuesta e WHERE e.mensaje.id = :mensajeId",
                Encuesta.class
            )
            .setParameter("mensajeId", mensajeId)
            .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<OpcionEncuesta> listarOpciones(int encuestaId) {
        return em.createQuery(
            "SELECT o FROM OpcionEncuesta o WHERE o.encuesta.envcuestaId = :encuestaId ORDER BY o.opcionId",
            OpcionEncuesta.class
        )
        .setParameter("encuestaId", encuestaId)
        .getResultList();
    }

    public long contarVotos(int opcionId) {
        return em.createQuery(
            "SELECT COUNT(v) FROM VotoEncuesta v WHERE v.opcion.opcionId = :opcionId",
            Long.class
        )
        .setParameter("opcionId", opcionId)
        .getSingleResult();
    }

    public VotoEncuesta buscarVotoDeUsuario(int encuestaId, int usuarioId) {
        try {
            return em.createQuery(
                "SELECT v FROM VotoEncuesta v " +
                "WHERE v.opcion.encuesta.envcuestaId = :encuestaId AND v.usuario.id = :usuarioId",
                VotoEncuesta.class
            )
            .setParameter("encuestaId", encuestaId)
            .setParameter("usuarioId", usuarioId)
            .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
