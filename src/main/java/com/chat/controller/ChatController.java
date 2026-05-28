package com.chat.controller;

import com.chat.service.ChatService;
import com.chat.datatype.CrearChatRequest;
import com.chat.datatype.AgregarMiembroRequest;
import com.chat.datatype.ChatDTO;
import com.chat.datatype.EliminarMiembroRequest;
import com.chat.model.Chat;
import com.chat.security.TokenService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/chats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatController {

    @Inject
    private ChatService chatService;

    @Inject
    private TokenService tokenService;

    @POST
    public Response crearChat(
        CrearChatRequest request,
        @HeaderParam("Authorization") String token
    ) {
        try {
            // VALIDAR TOKEN
            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            Long userId = tokenService.validarToken(token);

            // Validación básica
            if (request.getNombre() == null || request.getNombre().isBlank() ||
                request.getTipo() == null || request.getTipo().isBlank() ||
                request.getUsuarios() == null || request.getUsuarios().isEmpty()) {

                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Datos inválidos para crear el chat")
                        .build();
            }

            Chat chat = chatService.crearChat(
                request.getNombre(),
                request.getTipo(),
                request.getUsuarios(),
                userId
            );

            ChatDTO dto = new ChatDTO(
                chat.getChatId(),
                chat.getNombre(),
                "",
                "",
                0
            );

            return Response.ok(dto).build();

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token inválido")
                    .build();
            }
    }

    @GET
    public Response obtenerChats(@HeaderParam("Authorization") String token) {

        System.out.println("TOKEN RECIBIDO: [" + token + "]");

        try {
            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            Long userId = tokenService.validarToken(token);

            List<Chat> chats = chatService.obtenerChatsPorUsuario(userId);

            System.out.println("CHATS SIZE: " + chats.size());

            for (Chat c : chats) {
                System.out.println("CHAT ID: " + c.getChatId());
                System.out.println("MIEMBROS: " + (c.getMiembros() != null ? c.getMiembros().size() : "NULL"));
            }

            List<ChatDTO> resultado = chatService.obtenerChatsDTO(userId);

            return Response.ok(resultado).build();

        } catch (RuntimeException e) {

            e.printStackTrace();

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error interno en /chats")
                    .build();
        }
    }

    @POST
    @Path("/agregar-miembro")
    public Response agregarMiembro(
        AgregarMiembroRequest request,
        @HeaderParam("Authorization") String token
    ) {
        try {
            // VALIDAR TOKEN
            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            Long adminId = tokenService.validarToken(token);

            // Validación básica
            if (request == null ||
                request.getChatId() <= 0 ||
                request.getUsuarioId() <= 0) {

                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Datos inválidos")
                        .build();
            }

            chatService.agregarMiembro(
                request.getChatId(),
                adminId.intValue(), 
                request.getUsuarioId()
            );

            return Response.ok("Miembro agregado correctamente").build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token inválido")
                    .build();
        }
    }

    @POST
    @Path("/eliminar-miembro")
    public Response eliminarMiembro(
        EliminarMiembroRequest request,
        @HeaderParam("Authorization") String token
    ) {
        try {

            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            Long adminId = tokenService.validarToken(token);

            if (request == null ||
                request.getChatId() <= 0 ||
                request.getUsuarioId() <= 0) {

                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Datos inválidos")
                        .build();
            }

            chatService.eliminarMiembro(
                request.getChatId(),
                adminId.intValue(),
                request.getUsuarioId()
            );

            return Response.ok("Miembro eliminado").build();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }
}