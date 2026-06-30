package com.chat.service;

import com.chat.dao.ChatDAO;
import com.chat.dao.UsuarioDAO;
import com.chat.datatype.ChatDTO;
import com.chat.datatype.MiembroChatDTO;
import com.chat.dao.MensajeDAO;
import com.chat.model.*;
import com.chat.enums.ChatRol;
import com.chat.enums.TipoChat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@ApplicationScoped
public class ChatService {

    @Inject
    private ChatDAO chatDAO;

    @Inject
    private UsuarioDAO usuarioDAO;

    @Inject
    private MensajeDAO mensajeDAO;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Chat crearChat(String nombre, String tipo, List<Integer> usuariosIds, Long userId) {

        // 1. Validaciones básicas
        if (nombre == null || tipo == null || usuariosIds == null || usuariosIds.isEmpty()) {
            throw new IllegalArgumentException("Datos inválidos");
        }

        // 2. Validar tipo de chat
        TipoChat tipoChat;
        try {
            tipoChat = TipoChat.valueOf(tipo);
        } catch (Exception e) {
            throw new IllegalArgumentException("Tipo de chat inválido");
        }
        
        int cantidad = usuariosIds.size();

        if (tipoChat == TipoChat.PRIVADO) {
            if (cantidad != 2) {
                throw new RuntimeException("Un chat privado debe tener 2 usuarios");
            }
        }

        if (tipoChat == TipoChat.GRUPO) {
            if (cantidad < 2) {
                throw new RuntimeException("Un chat grupal debe tener al menos 2 usuarios");
            }
        } 

        if (tipoChat == TipoChat.INDIVIDUAL) {
            if (cantidad != 1) 
                throw new RuntimeException("Un chat individual debe tener 1 usuario");
            
            if (!usuariosIds.get(0).equals(userId.intValue())) 
                throw new RuntimeException("El chat individual debe ser del usuario logueado");

        }
        

        // 3. Validar que el creador esté en la lista
        if (!usuariosIds.contains(userId.intValue())) {
            throw new RuntimeException("El creador debe estar en la lista de usuarios");
        }

        // 4. Validar que los usuarios existan
        for (Integer userIdLista : usuariosIds) {

            Usuario usuario = usuarioDAO.buscarPorId(userIdLista);

            if (usuario == null) {
                throw new RuntimeException("Usuario no existe: " + userIdLista);
            }
        }

        // 5. Validar que no exista un chat privado con los mismos usuarios
        if (tipoChat == TipoChat.PRIVADO) {

            // caso 1: chat con uno mismo
            if (usuariosIds.size() == 1) {
                Integer user = usuariosIds.get(0);

                Chat existente = chatDAO.buscarChatPrivado(user, user);

                if (existente != null) {
                    return existente;
                }
            }

            // caso 2: chat entre dos usuarios
            if (usuariosIds.size() == 2) {
                Integer user1 = usuariosIds.get(0);
                Integer user2 = usuariosIds.get(1);

                Chat existente = chatDAO.buscarChatPrivado(user1, user2);

                if (existente != null) {
                    return existente;
                }
            }
        }

        // 6. Crear chat
        Chat chat = new Chat();
        chat.setNombre(nombre);
        chat.setTipo(tipoChat);

        chatDAO.guardar(chat);

        // IMPORTANTE: para tener el chatId
        em.flush();

        // 7. Crear miembros
        for (Integer userIdLista : usuariosIds) {

            Usuario usuario = usuarioDAO.buscarPorId(userIdLista);

            if (usuario == null) {
                throw new RuntimeException("Usuario no existe: " + userIdLista);
            }

            MiembroChat miembro = new MiembroChat();
            miembro.setChat(chat);
            miembro.setUsuario(usuario);

            // Definir rol correctamente
            if (userIdLista.equals(userId.intValue())) {
                miembro.setChatRol(ChatRol.CREADOR);
            } else {
                miembro.setChatRol(ChatRol.MIEMBRO);
            }

            // ID compuesto
            MiembroChatId id = new MiembroChatId(userIdLista, chat.getChatId());
            miembro.setId(id);

            em.persist(miembro);
        }

        return chat;
    }

    public List<Chat> obtenerChats() {
        return chatDAO.obtenerTodosChats();
    }

    public List<Chat> obtenerChatsPorUsuario(Long userId) {
        return chatDAO.obtenerChatsPorUsuario(userId.intValue());
    }

    public String obtenerUltimoMensaje(int chatId) {

        Mensaje mensaje =
            chatDAO.obtenerUltimoMensajeChat(chatId);

        if (mensaje == null) {
            return "";
        }

        switch (mensaje.getTipo()) {

            case IMAGEN:
                return "📷 Imagen";

            case VIDEO:
                return "🎥 Video";

        
            case AUDIO:
                return "🎤 Audio";

            case ARCHIVO:
                if (!mensaje.getAdjuntos().isEmpty()) {
                    return "📎 " +
                            mensaje.getAdjuntos()
                                .get(0)
                                .getNombreArchivo();
                }
                return "📎 Archivo";

            case ENCUESTA:
                return "📊 Encuesta";

            case TEXTO:
            default:
                return mensaje.getContenido();
        }
    }

    public List<ChatDTO> obtenerChatsDTO(Long userId) {

        List<Chat> chats =
            chatDAO.obtenerChatsPorUsuario(
                userId.intValue()
            );

            chats.sort((c1, c2) -> {

            Mensaje m1 = mensajeDAO.obtenerUltimoMensaje(c1.getChatId());
            Mensaje m2 = mensajeDAO.obtenerUltimoMensaje(c2.getChatId());

            if (m1 == null && m2 == null) return 0;
            if (m1 == null) return 1;
            if (m2 == null) return -1;

            return m2.getFechaEnviado().compareTo(m1.getFechaEnviado());
        });

         return chats.stream().map(chat -> {

          /*  MiembroChat miembro =
                chat.getMiembros().stream()
                    .filter(m ->
                        m.getUsuario().getId()
                        == userId.intValue()
                    )
                    .findFirst()
                    .orElse(null);

            int unread = 0;

            if (miembro != null) {

                if (miembro.getUltimoLeido() == null) {

                    unread = mensajeDAO
                        .contarTodos(
                            chat.getChatId(),
                            userId.intValue()
                        )
                        .intValue();

                } else {

                    unread = mensajeDAO
                        .contarNoLeidos(
                            chat.getChatId(),
                            userId.intValue(),
                            miembro.getUltimoLeido()
                        )
                        .intValue();
                }
            }

            return new ChatDTO(
                chat.getChatId(),
                obtenerNombre(chat, userId.intValue()),
                obtenerUltimoMensaje(chat.getChatId()),
                "",
                unread
            );

        }).toList();
    } */

        int unread =
            mensajeDAO.contarNoLeidos(
                chat.getChatId(),
                userId.intValue()
            ).intValue();

        return new ChatDTO(
            chat.getChatId(),
            obtenerNombre(
                chat,
                userId.intValue()
            ),
            obtenerUltimoMensaje(
                chat.getChatId()
            ),
            "",
            obtenerEstado(
                chat,
                userId.intValue()
            ),
            unread
        );

    }).toList();
}


     @Transactional
    public void agregarMiembro(int chatId, int adminId, int usuarioAgregarId) {

        // 1. Obtener chat
        Chat chat = chatDAO.buscarPorId(chatId);
        if (chat == null) {
            throw new RuntimeException("El chat no existe");
        }

        // 2. Validar tipo (solo grupal)
        if (chat.getTipo() != TipoChat.GRUPO) {
            throw new RuntimeException("Solo se pueden agregar miembros a chats grupales");
        }

        // 3. Verificar que el admin pertenece al chat
        MiembroChat admin = chatDAO.buscarMiembro(chatId, adminId);
        if (admin == null) {
            throw new RuntimeException("No perteneces al chat");
        }

        // 4. Verificar permisos
        if (admin.getChatRol() != ChatRol.ADMINISTRADOR && admin.getChatRol() != ChatRol.CREADOR) {
            throw new RuntimeException("No tienes permisos");
        }

        // 5. Verificar usuario a agregar
        Usuario usuario = usuarioDAO.buscarPorId(usuarioAgregarId);
        if (usuario == null) {
            throw new RuntimeException("Usuario no existe");
        }

        // 6. Verificar que no esté ya en el chat
        MiembroChat existente = chatDAO.buscarMiembro(chatId, usuarioAgregarId);
        if (existente != null) {
            throw new RuntimeException("El usuario ya está en el chat");
        }

        // 7. Crear miembro
        MiembroChat miembro = new MiembroChat();
        miembro.setChat(chat);
        miembro.setUsuario(usuario);
        miembro.setChatRol(ChatRol.MIEMBRO);

        MiembroChatId id = new MiembroChatId(usuarioAgregarId, chatId);
        miembro.setId(id);

        // 8. Guardar
        em.persist(miembro);
    }

    @Transactional
    public void eliminarMiembro(int chatId, int adminId, int usuarioEliminarId) {

        Chat chat = chatDAO.buscarPorId(chatId);
        if (chat == null) {
            throw new RuntimeException("El chat no existe");
        }

        // solo grupales
        if (chat.getTipo() != TipoChat.GRUPO) {
            throw new RuntimeException("Solo se pueden eliminar miembros en chats grupales");
        }

        // validar admin
        MiembroChat admin = chatDAO.buscarMiembro(chatId, adminId);
        if (admin == null) {
            throw new RuntimeException("No perteneces al chat");
        }

        if (admin.getChatRol() != ChatRol.ADMINISTRADOR &&
            admin.getChatRol() != ChatRol.CREADOR) {
            throw new RuntimeException("No tienes permisos");
        }

        // validar que exista el miembro
        MiembroChat miembro = chatDAO.buscarMiembro(chatId, usuarioEliminarId);
        if (miembro == null) {
            throw new RuntimeException("El usuario no pertenece al chat");
        }

        if (miembro.getChatRol() == ChatRol.CREADOR) {
        List<MiembroChat> miembros = chatDAO.obtenerMiembros(chatId);

        boolean hayOtroAdmin = miembros.stream().anyMatch(m ->
            m.getChatRol() == ChatRol.ADMINISTRADOR &&
            m.getUsuario().getId() != usuarioEliminarId
        );

        if (!hayOtroAdmin) {
            throw new RuntimeException("No se puede eliminar al creador sin otro administrador");
        }
    }
        // eliminar
        chatDAO.eliminarMiembro(chatId, usuarioEliminarId);
    }

    public List<MiembroChatDTO> listarMiembros(int chatId, int userId) {

        Chat chat = chatDAO.buscarPorId(chatId);
        if (chat == null) {
            throw new RuntimeException("El chat no existe");
        }

        MiembroChat solicitante = chatDAO.buscarMiembro(chatId, userId);
        if (solicitante == null) {
            throw new RuntimeException("No perteneces al chat");
        }

        return chatDAO.obtenerMiembros(chatId).stream()
            .map(m -> new MiembroChatDTO(
                m.getUsuario().getId(),
                m.getUsuario().getNombre(),
                m.getUsuario().getEmail(),
                m.getChatRol() != null ? m.getChatRol().name() : ChatRol.MIEMBRO.name()
            ))
            .toList();
    }

    @Transactional
    public void renombrarChat(int chatId, int userId, String nuevoNombre) {

        Chat chat = chatDAO.buscarPorId(chatId);
        if (chat == null) {
            throw new RuntimeException("El chat no existe");
        }

        if (chat.getTipo() != TipoChat.GRUPO) {
            throw new RuntimeException("Solo se pueden renombrar chats grupales");
        }

        MiembroChat miembro = chatDAO.buscarMiembro(chatId, userId);
        if (miembro == null) {
            throw new RuntimeException("No perteneces al chat");
        }

        chat.setNombre(nuevoNombre);
        em.merge(chat);
    }

    private String obtenerNombre(Chat chat, int usuarioActualId) {

        if (chat.getTipo() == TipoChat.PRIVADO) {

            List<MiembroChat> miembros =
                chat.getMiembros();

            return miembros.stream()
                .filter(m ->
                    m.getUsuario().getId()
                    != usuarioActualId
                )
                .map(m ->
                    m.getUsuario().getNombre()
                )
                .findFirst()
                .orElse(chat.getNombre());
        }

        return chat.getNombre();
    }


    private String obtenerEstado(Chat chat, int usuarioActualId) {

        if (chat.getTipo() == TipoChat.PRIVADO) {

            return chat.getMiembros().stream()
                .filter(m ->
                    m.getUsuario().getId()
                    != usuarioActualId
                )
                .map(m ->
                    m.getUsuario().getEstado().name()
                )
                .findFirst()
                .orElse("OFFLINE");
        }

        return null;
    }

}