package com.chat.service;

import com.chat.dao.MensajeDAO;
import com.chat.dao.ChatDAO;
import com.chat.dao.UsuarioDAO;
import com.chat.dao.MensajeUsuarioDAO;
import com.chat.datatype.MensajeResponse;
import com.chat.model.*;
import com.chat.enums.TipoMensaje;
import com.chat.enums.EstadoMensaje;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import com.chat.websocket.ChatWebSocket;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class MensajeService {

    @Inject
    private MensajeDAO mensajeDAO;
    @Inject
    private ChatDAO chatDAO;
    @Inject
    private UsuarioDAO usuarioDAO;
    @Inject
    private MensajeUsuarioDAO mensajeUsuarioDAO;

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

        // 4. crear registros en MensajeUsuario para cada receptor
        for (MiembroChat miembro : chat.getMiembros()) {
            Usuario receptor = miembro.getUsuario();

            if (receptor.getId() == usuario.getId()) {
                continue;
            }

            MensajeUsuario mu = new MensajeUsuario();

            mu.setMensaje(mensaje);
            mu.setReceptor(receptor);
            mu.setEliminado(false); 

            mensajeUsuarioDAO.guardar(mu);
        }

        // 4. enviar por WebSocket
        String contenidoSeguro = contenido
            .replace("\"", "\\\"")
            .replace("\n", " ");

        String json = String.format(
             """
            {
                "id": "%d",
                "chatId": "%d",
                "remitente": "%s",
                "remitenteId": "%d",
                "contenido": "%s",
                "timestamp": "%s"
            }
            """,
            mensaje.getId(),
            chat.getChatId(),
            usuario.getNombre(),
            usuario.getId(),
            contenidoSeguro,
            mensaje.getFechaEnviado()
        );

        List<Integer> usuarios =
            chat.getMiembros()
                .stream()
                .map(m ->
                    m.getUsuario().getId()
                )
                .toList();

        ChatWebSocket.sendToUsers(
            usuarios,
            json
        );
    }

    public List<MensajeResponse> listar(int chatId, int userId) {
        
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
            dto.entregado = Boolean.TRUE.equals(fueEntregado(m.getId()));
            dto.leido = Boolean.TRUE.equals(fueLeido(m.getId()));
            return dto;
        }).toList();
    }

    public boolean usuarioPerteneceAlChat(int chatId, int usuarioId) {

        Chat chat = chatDAO.buscarPorId(chatId);

        if (chat == null) {
            return false;
        }

        return chat.getMiembros().stream()
                .anyMatch(m ->
                        m.getUsuario().getId() == usuarioId
                );

    }

    @Transactional
    public void marcarComoLeido(int chatId, int usuarioId) {

        MiembroChat miembro =
            chatDAO.buscarMiembro(chatId, usuarioId);

        if (miembro != null) {

            miembro.setUltimoLeido(
                java.time.LocalDateTime.now()
            );
        }
    }

    public boolean fueEntregado(int mensajeId) {
        List<MensajeUsuario> lista =
            mensajeUsuarioDAO.listarPorMensaje(mensajeId);

        if (lista.isEmpty()) {
            return false;
        }

        return lista.stream()
            .allMatch(mu ->
                mu.getFechaEntregado() != null
            );
    }

    public boolean fueLeido(int mensajeId) {
        List<MensajeUsuario> lista =
            mensajeUsuarioDAO.listarPorMensaje(mensajeId);

        if (lista.isEmpty()) {
            return false;
        }

        return lista.stream()
            .allMatch(mu ->
                mu.getFechaLeido() != null
            );
    }

    @Transactional
    public void marcarEntregado(int mensajeId, int usuarioId) {

        MensajeUsuario mu =
            mensajeUsuarioDAO.buscarPorMensajeYUsuario(
                mensajeId,
                usuarioId
            );

        if (mu != null && mu.getFechaEntregado() == null) {

            mu.setFechaEntregado(LocalDateTime.now());
            mensajeUsuarioDAO.update(mu);

            if (fueEntregado(mensajeId)) {

                Mensaje mensaje =
                    mensajeDAO.buscarPorId(mensajeId);

                int emisorId =
                    mensaje.getEmisor().getId();

                String json = String.format(
                    """
                    {
                        "type":"message_delivered",
                        "messageId":"%d"
                    }
                    """,
                    mensajeId
                );

                ChatWebSocket.sendToUsers(
                    List.of(emisorId),
                    json
                );
            }
        }
    }

   @Transactional
    public void marcarLeido(int chatId, int usuarioId) {

        List<MensajeUsuario> lista =
            mensajeUsuarioDAO.findNoLeidos(
                chatId,
                usuarioId
            );

        for (MensajeUsuario mu : lista) {

            if (mu.getFechaEntregado() == null) {
                mu.setFechaEntregado(LocalDateTime.now());
            }

            mu.setFechaLeido(LocalDateTime.now());
            mensajeUsuarioDAO.update(mu);

            int mensajeId =
                mu.getMensaje().getId();

            if (fueLeido(mensajeId)) {

                Mensaje mensaje =
                    mu.getMensaje();

                int emisorId =
                    mensaje.getEmisor().getId();

                String json = String.format(
                    """
                    {
                        "type":"message_read",
                        "messageId":"%d"
                    }
                    """,
                    mensajeId
                );

                ChatWebSocket.sendToUsers(
                    List.of(emisorId),
                    json
                );
            }
        }
    }
}