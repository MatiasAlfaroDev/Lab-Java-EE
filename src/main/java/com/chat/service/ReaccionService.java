package com.chat.service;

import com.chat.dao.MensajeDAO;
import com.chat.dao.ReaccionDAO;
import com.chat.dao.UsuarioDAO;
import com.chat.datatype.ReaccionWsEvent;
import com.chat.model.Mensaje;
import com.chat.model.ReaccionMensaje;
import com.chat.model.Usuario;
import com.chat.websocket.ChatWebSocket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@ApplicationScoped
public class ReaccionService {

    @Inject
    private ReaccionDAO reaccionDAO;

    @Inject
    private MensajeDAO mensajeDAO;

    @Inject
    private UsuarioDAO usuarioDAO;

    private final ObjectMapper mapper =
    new ObjectMapper();

    private void notificarReaccion(
    Mensaje mensaje,
    int usuarioId,
    String emoji
) {

    try {

        ReaccionWsEvent event =
            new ReaccionWsEvent(
                "MESSAGE_REACTION",
                mensaje.getId(),
                usuarioId,
                emoji
            );

        String json =
            mapper.writeValueAsString(event);

        List<Integer> usuarios =
            mensajeDAO.findUsuariosByChatId(
                mensaje.getChat().getChatId()
            );

        ChatWebSocket.sendToUsers(
            usuarios,
            json
        );

    } catch (Exception e) {

        e.printStackTrace();
    }
}

    @Transactional
    public void reaccionar(
            int mensajeId,
            int usuarioId,
            String emoji
    ) {

        Mensaje mensaje =
                mensajeDAO.buscarPorId(mensajeId);

        if (mensaje == null) {
            throw new RuntimeException("Mensaje no encontrado");
        }

        Usuario usuario =
                usuarioDAO.buscarPorId(usuarioId);

        if (usuario == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        ReaccionMensaje reaccionExistente =
                reaccionDAO.buscarPorMensajeYUsuario(
                        mensajeId,
                        usuarioId
                );

        // NO EXISTE -> CREAR
        if (reaccionExistente == null) {

            ReaccionMensaje nueva =
                    new ReaccionMensaje();

            nueva.setMensaje(mensaje);
            nueva.setUsuarioReaccion(usuario);
            nueva.setEmojiString(emoji);

            reaccionDAO.guardar(nueva);
            notificarReaccion(mensaje, usuarioId, emoji);

            return;
        }

        // MISMO EMOJI -> ELIMINAR
        if (reaccionExistente.getEmojiString().equals(emoji)) {

            reaccionDAO.eliminar(reaccionExistente);
            notificarReaccion(mensaje, usuarioId, null);
            return;
        }

        // DISTINTO EMOJI -> ACTUALIZAR
        reaccionExistente.setEmojiString(emoji);

        reaccionDAO.actualizar(reaccionExistente);
        notificarReaccion(mensaje, usuarioId, emoji);
    }
}