package com.chat.dao;

import com.chat.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UsuarioDAO {

    @PersistenceContext
    private EntityManager em;

    // Guardar usuario
    @Transactional
    public void guardar(Usuario usuario) {
        em.persist(usuario);
    }

    // Buscar por email
    public Usuario buscarEmail(String email) {
        try {
            return em.createQuery(
                    "SELECT u FROM Usuario u WHERE u.email = :email",
                    Usuario.class
            )
            .setParameter("email", email)
            .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }

    // Verificar si existe email
    public boolean existeEmail(String email) {
        return buscarEmail(email) != null;
    }
}