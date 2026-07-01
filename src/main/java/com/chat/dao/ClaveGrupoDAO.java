package com.chat.dao;

import com.chat.model.ClaveGrupo;
import com.chat.datatype.ClaveGrupoRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
@ApplicationScoped
public class ClaveGrupoDAO {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void guardarOActualizar(int chatId, int miembroId, int distribuidorId,
                                   String claveEnvuelta, long version) {
        em.createQuery(
            "DELETE FROM ClaveGrupo c WHERE c.chatId = :chatId AND c.miembroId = :miembroId")
          .setParameter("chatId", chatId)
          .setParameter("miembroId", miembroId)
          .executeUpdate();

        ClaveGrupo cg = new ClaveGrupo();
        cg.setChatId(chatId);
        cg.setMiembroId(miembroId);
        cg.setDistribuidorId(distribuidorId);
        cg.setClaveEnvuelta(claveEnvuelta);
        cg.setVersion(version);
        em.persist(cg);
    }

    // Escribe todas las filas de una distribución en UNA sola transacción: si dos
    // distribuciones concurrentes (dos clientes reparando a la vez, o un doble click)
    // llegan a la vez, una queda completa y la otra completa — nunca una mezcla de
    // filas de ambas, que dejaba a todos con claves distintas entre sí.
    @Transactional
    public void guardarOActualizarLote(int chatId, List<ClaveGrupoRequest.Envuelta> envueltas,
                                        int distribuidorId, long version) {
        for (ClaveGrupoRequest.Envuelta e : envueltas) {
            em.createQuery(
                "DELETE FROM ClaveGrupo c WHERE c.chatId = :chatId AND c.miembroId = :miembroId")
              .setParameter("chatId", chatId)
              .setParameter("miembroId", e.miembroId)
              .executeUpdate();

            ClaveGrupo cg = new ClaveGrupo();
            cg.setChatId(chatId);
            cg.setMiembroId(e.miembroId);
            cg.setDistribuidorId(distribuidorId);
            cg.setClaveEnvuelta(e.claveEnvuelta);
            cg.setVersion(version);
            em.persist(cg);
        }
    }

    @Transactional
    public void eliminarPorMiembro(int miembroId) {
        em.createQuery("DELETE FROM ClaveGrupo c WHERE c.miembroId = :miembroId")
          .setParameter("miembroId", miembroId)
          .executeUpdate();
    }

    public ClaveGrupo buscarPorMiembro(int chatId, int miembroId) {
        try {
            return em.createQuery(
                "SELECT c FROM ClaveGrupo c WHERE c.chatId = :chatId AND c.miembroId = :miembroId",
                ClaveGrupo.class)
              .setParameter("chatId", chatId)
              .setParameter("miembroId", miembroId)
              .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
