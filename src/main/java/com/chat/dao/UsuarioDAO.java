package com.chat.dao;

import com.chat.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

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

    // Actualizar usuario
    @Transactional
    public Usuario actualizar(Usuario usuario) {
        return em.merge(usuario);
    }

    public Usuario buscarPorId(int id) {
    return em.find(Usuario.class, id);
    }

    public List<Usuario> listar() {
        return em.createQuery("""
            SELECT u
            FROM Usuario u
            WHERE u.rol <> 'ADMIN'
            AND u.bloqueado = false
            """, Usuario.class)
            .getResultList();
    }

    public List<Usuario> listarTodos() {
        return em.createQuery("SELECT u FROM Usuario u ORDER BY u.nombre", Usuario.class)
            .getResultList();
    }

    @Transactional
    public void limpiarToken(String pushToken) {

        em.createQuery("""
            UPDATE Usuario u
            SET u.pushToken = null
            WHERE u.pushToken = :token
        """)
        .setParameter("token", pushToken)
        .executeUpdate();
    }

    @Transactional
    public void guardarPublicKey(int usuarioId, String publicKey) {
        em.createQuery("UPDATE Usuario u SET u.publicKey = :key WHERE u.id = :id")
          .setParameter("key", publicKey)
          .setParameter("id", usuarioId)
          .executeUpdate();
    }

    public String buscarPublicKey(int usuarioId) {
        Usuario u = em.find(Usuario.class, usuarioId);
        return u != null ? u.getPublicKey() : null;
    }
}