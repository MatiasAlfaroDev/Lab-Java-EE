package com.chat.dao;

import com.chat.model.ClaveGrupo;
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
                                   String claveEnvuelta, int version) {
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
