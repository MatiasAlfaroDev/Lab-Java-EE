package com.chat.service;

import com.chat.dao.MensajeDAO;
import com.chat.dao.ChatDAO;
import com.chat.dao.UsuarioDAO;
import com.chat.datatype.MensajeResponse;
import com.chat.model.*;
import com.chat.enums.TipoMensaje;
import com.chat.enums.EstadoMensaje;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import com.chat.websocket.ChatWebSocket;
import java.util.List;

@ApplicationScoped
public class MensajeService {

    @Inject
    private MensajeDAO mensajeDAO;

    @Inject
    private ChatDAO chatDAO;

    @Inject
    private UsuarioDAO usuarioDAO;

   /* @Transactional
    public void enviarMensaje(int chatId, int userId, String contenido, TipoMensaje tipo) {

        Mensaje mensaje = new Mensaje();
        try {
            // 1. validaciones
            Chat chat = chatDAO.buscarPorId(chatId);
            Usuario usuario = usuarioDAO.buscarPorId(userId);

            if (chat == null || usuario == null) {
                throw new RuntimeException("Datos inválidos");
            }

            boolean pertenece = chat.getMiembros()
                    .stream()
                    .anyMatch(m -> m.getUsuario().getId() == userId);

            if (!pertenece) {
                throw new RuntimeException("No pertenece al chat");
            }

            mensaje.setChat(chat);
            mensaje.setEmisor(usuario);
            mensaje.setContenido(contenido);
            mensaje.setTipo(tipo);
            mensaje.setEstado(EstadoMensaje.ENVIADO);

        } catch (Exception e) {
            mensaje.setEstado(EstadoMensaje.RECHAZADO);
            mensaje.setContenido(contenido); 
        }

        mensajeDAO.guardar(mensaje);
    } */

    @Transactional
    public void enviarMensaje(int chatId, int userId, String contenido, TipoMensaje tipo) {

        // 1. validaciones
        Chat chat = chatDAO.buscarPorId(chatId);
        Usuario usuario = usuarioDAO.buscarPorId(userId);

        if (chat == null || usuario == null) {
            throw new RuntimeException("Datos inválidos");
        }

        boolean pertenece = chat.getMiembros()
                .stream()
                .anyMatch(m -> m.getUsuario().getId() == userId);

        if (!pertenece) {
            throw new RuntimeException("No pertenece al chat");
        }

        // 2. crear mensaje
        Mensaje mensaje = new Mensaje();

        mensaje.setChat(chat);
        mensaje.setEmisor(usuario);
        mensaje.setContenido(contenido);
        mensaje.setTipo(tipo);
        mensaje.setEstado(EstadoMensaje.ENVIADO);

        // 3. guardar
        mensajeDAO.guardar(mensaje);

        // 4. enviar por WebSocket
        String json = String.format(
             """
            {
                "id": "%d",
                "chatId": "%d",
                "remitente": "%s",
                "remitenteId": "%d",
                "contenido": "%s"
            }
            """,
            mensaje.getId(),
            chat.getChatId(),
            usuario.getNombre(),
            usuario.getId(),
            contenido
        );

        ChatWebSocket.broadcast(json);
        }

    public List<MensajeResponse> listar(int chatId) {

        List<Mensaje> mensajes =
            mensajeDAO.listarPorChat(chatId);

        return mensajes.stream().map(m -> {
            MensajeResponse dto =
                new MensajeResponse();
            dto.id = m.getId();
            dto.chatId =
                m.getChat().getChatId();
            dto.sender_id =
                m.getEmisor().getId();
            dto.sender_username =
                m.getEmisor().getNombre();
            dto.sender_initials =
                m.getEmisor()
                .getNombre()
                .substring(0, 2)
                .toUpperCase();
            dto.contenido =
                m.getContenido();
            dto.sent_at =
                m.getFechaEnviado().toString();
            dto.estado =
                m.getEstado().toString();
            return dto;
        }).toList();
    }
}