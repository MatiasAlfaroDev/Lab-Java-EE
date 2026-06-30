package com.chat.service;

import com.chat.dao.MensajeDAO;
import com.chat.dao.AdjuntoDAO;
import com.chat.dao.ChatDAO;
import com.chat.dao.UsuarioDAO;
import com.chat.dao.MensajeUsuarioDAO;
import com.chat.datatype.MensajeResponse;
import com.chat.datatype.ReaccionDTO;
import com.chat.datatype.UploadAdjuntoResponse;
import com.chat.model.*;
import com.chat.enums.TipoMensaje;
import com.chat.enums.EstadoMensaje;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import com.chat.websocket.ChatWebSocket;
import com.chat.datatype.PushTokenDTO;
import com.chat.dao.AdjuntoDAO;

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
    @Inject
    private PushNotificationService pushNotificationService;
    @Inject
    private AdjuntoDAO adjuntoDAO;
    @PersistenceContext
    private EntityManager em;


    @Transactional
    public void enviarMensaje(int chatId, int userId, String contenido, TipoMensaje tipo, String nombreArchivo, Long tamanoArchivo, String mimeType) {

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
        if (tipo == TipoMensaje.ARCHIVO) {
            if (mimeType != null && mimeType.startsWith("image/")) {
                mensaje.setTipo(TipoMensaje.IMAGEN);
            } else if (mimeType != null && mimeType.startsWith("video/")) {
                mensaje.setTipo(TipoMensaje.VIDEO);
            } else {
                mensaje.setTipo(TipoMensaje.ARCHIVO);
            }
        } else {
            mensaje.setTipo(tipo);
        }
        mensaje.setEstado(EstadoMensaje.ENVIADO);

        // 3. guardar
        mensajeDAO.guardar(mensaje);
        em.flush(); // asegurar que el ID se genere antes de continuar

        if (tipo == TipoMensaje.ARCHIVO
                || tipo == TipoMensaje.IMAGEN
                || tipo == TipoMensaje.VIDEO
                || tipo == TipoMensaje.AUDIO) {

            Adjunto adjunto = new Adjunto();

            adjunto.setMensajeReferencia(mensaje);
            adjunto.setNombreArchivo(nombreArchivo);
            adjunto.setUrlArchivo(contenido); // contenido contiene la URL
            adjunto.setTamanoArchivo(tamanoArchivo);
            adjunto.setMimeType(mimeType);

            adjuntoDAO.guardar(adjunto);
        }

        // 4. crear registros en MensajeUsuario para cada receptor
        for (MiembroChat miembro : chat.getMiembros()) {
            Usuario receptor = miembro.getUsuario();

                MensajeUsuario mu = new MensajeUsuario();

                mu.setMensaje(mensaje);
                mu.setReceptor(receptor);
                mu.setEliminado(false); 

                mensajeUsuarioDAO.guardar(mu);
        }

        em.flush(); // asegurar que los registros de MensajeUsuario se guarden antes de enviar notificaciones

        // 5. enviar por WebSocket
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

        // 6. enviar push notifications
        for (MiembroChat miembro : chat.getMiembros()) {

            Usuario receptor =
                miembro.getUsuario();

            // no enviarse push a sí mismo
            if (receptor.getId() == userId) {
                continue;
            }

            if (
                receptor.getPushToken() != null &&
                !receptor.getPushToken().isBlank()
            ) {

                pushNotificationService.enviarPush(
                    receptor.getPushToken(),
                    usuario.getNombre(),
                    contenido
                );

                System.out.println(
                    "PUSH ENVIADA A "
                    + receptor.getNombre()
                );
            }
        }
    }

    public List<MensajeResponse> listar(int chatId, int userId) {
        
        List<Mensaje> mensajes =
            mensajeDAO.listarPorChat(chatId, userId);

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
            dto.tipo = m.getTipo().name();
            if (!m.getAdjuntos().isEmpty()) {

                Adjunto adjunto = m.getAdjuntos().get(0);

                dto.adjunto = new UploadAdjuntoResponse(
                    adjunto.getUrlArchivo(),
                    adjunto.getNombreArchivo(),
                    adjunto.getTamanoArchivo(),
                    adjunto.getMimeType()
                );
            }
            dto.sent_at =
                m.getFechaEnviado().toString();
            dto.estado =
                m.getEstado().toString();
            dto.entregado = Boolean.TRUE.equals(fueEntregado(m.getId()));
            dto.leido = Boolean.TRUE.equals(fueLeido(m.getId()));
            dto.editado = m.isEditado();
            dto.eliminado = m.isEliminado();
            dto.reacciones = m.getReacciones().stream().map(r -> {
            
            ReaccionDTO dtoR = new ReaccionDTO();
            dtoR.usuarioId = r.getUsuarioReaccion().getId();
            dtoR.usuarioNombre = r.getUsuarioReaccion().getNombre();
            dtoR.emoji = r.getEmojiString();
            return dtoR;
            }).toList();
            dto.mensajeOrigenId = m.getMensajeOrigen() != null ? m.getMensajeOrigen().getId() : 0;
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

    @Transactional
    public Mensaje editarMensaje(int mensajeId, int usuarioId, String nuevoContenido) {

        Mensaje mensaje = mensajeDAO.buscarPorId(mensajeId);

        if (mensaje == null) {
            throw new RuntimeException("Mensaje no existe");
        }

        if (mensaje.getEmisor().getId() != usuarioId) {
            throw new RuntimeException("No tienes permiso");
        }

        if (mensaje.getTipo() != TipoMensaje.TEXTO) {
            throw new RuntimeException("Solo texto");
        }

        mensaje.setContenido(nuevoContenido);
        mensaje.setEditado(true);

        mensajeDAO.update(mensaje);

        String contenidoSeguro = nuevoContenido
            .replace("\"", "\\\"")
            .replace("\n", " ");

        String json = String.format(
            """
            {
                "type":"message_edited",
                "messageId":"%d",
                "chatId":"%d",
                "contenido":"%s",
                "editado":true
            }
            """,
            mensaje.getId(),
            mensaje.getChat().getChatId(),
            contenidoSeguro
        );

        List<Integer> usuarios =
            mensaje.getChat()
                .getMiembros()
                .stream()
                .map(m -> m.getUsuario().getId())
                .toList();

        ChatWebSocket.sendToUsers(usuarios, json);

        return null;
    }

    @Transactional
    public void eliminarParaMi(int mensajeId, int usuarioId) {

        Mensaje mensaje =
            mensajeDAO.buscarPorId(mensajeId);

        if (mensaje == null) {
            throw new RuntimeException("Mensaje no existe");
        }

        mensajeUsuarioDAO.eliminarParaUsuario(
            mensajeId,
            usuarioId
        );
    }
    
    @Transactional
    public void eliminarParaTodos(int mensajeId, int usuarioId) {

        Mensaje mensaje = mensajeDAO.buscarPorId(mensajeId);

        if (mensaje == null) {
            throw new RuntimeException("Mensaje no existe");
        }

        // solo el emisor puede eliminar para todos
        if (mensaje.getEmisor().getId() != usuarioId) {
            throw new RuntimeException("No tienes permiso");
        }

        mensaje.setEliminado(true);
        mensajeDAO.update(mensaje);

        // websocket para actualización en tiempo real
        String json = String.format(
            """
            {
                "type":"message_deleted",
                "messageId":"%d",
                "chatId":"%d",
                "eliminado":true
            }
            """,
            mensaje.getId(),
            mensaje.getChat().getChatId()
        );

        List<Integer> usuarios =
            mensaje.getChat()
                .getMiembros()
                .stream()
                .map(m -> m.getUsuario().getId())
                .toList();

        ChatWebSocket.sendToUsers(usuarios, json);
    }

    @Transactional
    public void reenviarMensaje(int mensajeId, int chatId, int userId) {

        // 1. validar datos
        Mensaje original = mensajeDAO.buscarPorId(mensajeId);
        Chat chat = chatDAO.buscarPorId(chatId);
        Usuario usuario = usuarioDAO.buscarPorId(userId);

        if (original == null || chat == null || usuario == null) {
            throw new RuntimeException("Datos inválidos");
        }

        boolean pertenece = chat.getMiembros()
                .stream()
                .anyMatch(m -> m.getUsuario().getId() == userId);

        if (!pertenece) {
            throw new RuntimeException("No pertenece al chat");
        }

        // 2. crear mensaje nuevo
        Mensaje mensaje = new Mensaje();
        mensaje.setChat(chat);
        mensaje.setEmisor(usuario);
        mensaje.setContenido(original.getContenido());
        mensaje.setTipo(original.getTipo());
        mensaje.setEstado(EstadoMensaje.ENVIADO);

        // 🔥 clave del forward
        mensaje.setMensajeOrigen(original);

        // 3. guardar
        mensajeDAO.guardar(mensaje);
        em.flush();
        if (!original.getAdjuntos().isEmpty()) {

            Adjunto originalAdjunto =
                original.getAdjuntos().get(0);

            Adjunto nuevo = new Adjunto();

            nuevo.setMensajeReferencia(mensaje);
            nuevo.setNombreArchivo(originalAdjunto.getNombreArchivo());
            nuevo.setUrlArchivo(originalAdjunto.getUrlArchivo());
            nuevo.setTamanoArchivo(originalAdjunto.getTamanoArchivo());
            nuevo.setMimeType(originalAdjunto.getMimeType());

            adjuntoDAO.guardar(nuevo);
        }

        // 4. crear registros MensajeUsuario
        for (MiembroChat miembro : chat.getMiembros()) {

            MensajeUsuario mu = new MensajeUsuario();
            mu.setMensaje(mensaje);
            mu.setReceptor(miembro.getUsuario());
            mu.setEliminado(false);

            mensajeUsuarioDAO.guardar(mu);
        }

        // 5. websocket
        String contenidoSeguro = mensaje.getContenido()
            .replace("\"", "\\\"")
            .replace("\n", " ");

        String json = String.format(
            """
            {
                "type":"message_forwarded",
                "id":"%d",
                "chatId":"%d",
                "remitente":"%s",
                "remitenteId":"%d",
                "contenido":"%s",
                "timestamp":"%s",
                "originalMessageId":"%d"
            }
            """,
            mensaje.getId(),
            chat.getChatId(),
            usuario.getNombre(),
            usuario.getId(),
            contenidoSeguro,
            mensaje.getFechaEnviado(),
            original.getId()
        );

        List<Integer> usuarios =
            chat.getMiembros()
                .stream()
                .map(m -> m.getUsuario().getId())
                .toList();

        ChatWebSocket.sendToUsers(usuarios, json);
    }
}