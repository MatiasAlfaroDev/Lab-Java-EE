package com.chat.service;

import com.chat.dao.ChatDAO;
import com.chat.dao.EncuestaDAO;
import com.chat.dao.MensajeDAO;
import com.chat.dao.MensajeUsuarioDAO;
import com.chat.dao.UsuarioDAO;
import com.chat.datatype.EncuestaDTO;
import com.chat.enums.EstadoMensaje;
import com.chat.enums.TipoMensaje;
import com.chat.model.*;
import com.chat.websocket.ChatWebSocket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class EncuestaService {

    @Inject
    private EncuestaDAO encuestaDAO;
    @Inject
    private ChatDAO chatDAO;
    @Inject
    private UsuarioDAO usuarioDAO;
    @Inject
    private MensajeDAO mensajeDAO;
    @Inject
    private MensajeUsuarioDAO mensajeUsuarioDAO;
    @PersistenceContext
    private EntityManager em;

    @Transactional
    public EncuestaDTO crearEncuesta(
            int chatId, int creadorId, String pregunta,
            List<String> opciones, boolean anonima, String expiresAtStr) {

        Chat chat = chatDAO.buscarPorId(chatId);
        Usuario creador = usuarioDAO.buscarPorId(creadorId);

        if (chat == null || creador == null) {
            throw new RuntimeException("Datos inválidos");
        }

        boolean pertenece = chat.getMiembros().stream()
                .anyMatch(m -> m.getUsuario().getId() == creadorId);
        if (!pertenece) {
            throw new RuntimeException("No pertenece al chat");
        }

        if (pregunta == null || pregunta.isBlank() || opciones == null || opciones.size() < 2) {
            throw new RuntimeException("Datos inválidos");
        }

        LocalDateTime expiresAt = null;
        if (expiresAtStr != null && !expiresAtStr.isBlank()) {
            try {
                expiresAt = LocalDateTime.parse(expiresAtStr, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                throw new RuntimeException("expires_at inválido");
            }
        }

        // 1. mensaje que ancla la encuesta en el historial del chat
        Mensaje mensaje = new Mensaje();
        mensaje.setChat(chat);
        mensaje.setEmisor(creador);
        mensaje.setContenido(pregunta);
        mensaje.setTipo(TipoMensaje.ENCUESTA);
        mensaje.setEstado(EstadoMensaje.ENVIADO);
        mensajeDAO.guardar(mensaje);
        em.flush();

        for (MiembroChat miembro : chat.getMiembros()) {
            MensajeUsuario mu = new MensajeUsuario();
            mu.setMensaje(mensaje);
            mu.setReceptor(miembro.getUsuario());
            mu.setEliminado(false);
            mensajeUsuarioDAO.guardar(mu);
        }

        // 2. encuesta + opciones
        Encuesta encuesta = new Encuesta();
        encuesta.setChatId(chat);
        encuesta.setCreadorEncuesta(creador);
        encuesta.setMensaje(mensaje);
        encuesta.setPregunta(pregunta);
        encuesta.setAnonima(anonima);
        encuesta.setExpiresAt(expiresAt);
        encuestaDAO.guardar(encuesta);
        em.flush();

        for (String texto : opciones) {
            OpcionEncuesta opcion = new OpcionEncuesta();
            opcion.setEncuesta(encuesta);
            opcion.setTextoOpcion(texto);
            encuestaDAO.guardarOpcion(opcion);
        }
        em.flush();

        // 3. avisar por WS (mínimo: que el chat se refresque y traiga la encuesta)
        String contenidoSeguro = pregunta.replace("\"", "\\\"").replace("\n", " ");
        String json = String.format(
            """
            {
                "type":"POLL_NEW",
                "id": "%d",
                "chatId": "%d",
                "remitente": "%s",
                "remitenteId": "%d",
                "contenido": "%s",
                "timestamp": "%s"
            }
            """,
            mensaje.getId(), chat.getChatId(), creador.getNombre(), creador.getId(),
            contenidoSeguro, mensaje.getFechaEnviado()
        );

        List<Integer> usuarios = chat.getMiembros().stream()
                .map(m -> m.getUsuario().getId()).toList();
        ChatWebSocket.sendToUsers(usuarios, json);

        return construirDTO(encuesta, creadorId);
    }

    @Transactional
    public EncuestaDTO votar(int encuestaId, int opcionId, int usuarioId) {

        Encuesta encuesta = encuestaDAO.buscarPorId(encuestaId);
        if (encuesta == null) {
            throw new RuntimeException("Encuesta no existe");
        }

        OpcionEncuesta opcion = encuestaDAO.buscarOpcionPorId(opcionId);
        if (opcion == null || opcion.getEncuesta().getEnvcuestaId() != encuestaId) {
            throw new RuntimeException("Opción inválida");
        }

        boolean pertenece = encuesta.getChatId().getMiembros().stream()
                .anyMatch(m -> m.getUsuario().getId() == usuarioId);
        if (!pertenece) {
            throw new RuntimeException("No pertenece al chat");
        }

        if (encuesta.getExpiresAt() != null && LocalDateTime.now().isAfter(encuesta.getExpiresAt())) {
            throw new RuntimeException("Encuesta expirada");
        }

        if (encuestaDAO.buscarVotoDeUsuario(encuestaId, usuarioId) != null) {
            throw new RuntimeException("Ya votaste en esta encuesta");
        }

        Usuario usuario = usuarioDAO.buscarPorId(usuarioId);
        VotoEncuesta voto = new VotoEncuesta();
        voto.setOpcion(opcion);
        voto.setUsuario(usuario);
        encuestaDAO.guardarVoto(voto);
        em.flush();

        EncuestaDTO dto = construirDTO(encuesta, usuarioId);

        String json = String.format(
            "{\"type\":\"POLL_VOTE\",\"pollId\":%d,\"chatId\":%d,\"total\":%d}",
            encuestaId, encuesta.getChatId().getChatId(), dto.total
        );
        List<Integer> usuarios = encuesta.getChatId().getMiembros().stream()
                .map(m -> m.getUsuario().getId()).toList();
        ChatWebSocket.sendToUsers(usuarios, json);

        return dto;
    }

    public EncuestaDTO obtenerPorMensaje(int mensajeId, int usuarioId) {
        Encuesta encuesta = encuestaDAO.buscarPorMensajeId(mensajeId);
        return encuesta != null ? construirDTO(encuesta, usuarioId) : null;
    }

    public EncuestaDTO resultados(int encuestaId, int usuarioId) {
        Encuesta encuesta = encuestaDAO.buscarPorId(encuestaId);
        if (encuesta == null) {
            throw new RuntimeException("Encuesta no existe");
        }
        return construirDTO(encuesta, usuarioId);
    }

    public EncuestaDTO construirDTO(Encuesta encuesta, Integer usuarioId) {
        EncuestaDTO dto = new EncuestaDTO();
        dto.id = encuesta.getEnvcuestaId();
        dto.pregunta = encuesta.getPregunta();
        dto.anonima = encuesta.isAnonima();
        dto.expires_at = encuesta.getExpiresAt() != null ? encuesta.getExpiresAt().toString() : null;

        List<OpcionEncuesta> opciones = encuestaDAO.listarOpciones(encuesta.getEnvcuestaId());
        long total = 0;
        dto.opciones = new java.util.ArrayList<>();
        for (OpcionEncuesta o : opciones) {
            long votos = encuestaDAO.contarVotos(o.getOpcionId());
            total += votos;
            dto.opciones.add(new EncuestaDTO.OpcionDTO(o.getOpcionId(), o.getTextoOpcion(), votos));
        }
        dto.total = total;

        if (usuarioId != null) {
            VotoEncuesta miVoto = encuestaDAO.buscarVotoDeUsuario(encuesta.getEnvcuestaId(), usuarioId);
            dto.myVote = miVoto != null ? miVoto.getOpcion().getOpcionId() : null;
        }

        return dto;
    }
}
