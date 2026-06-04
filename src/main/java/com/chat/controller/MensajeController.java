package com.chat.controller;

import com.chat.service.MensajeService;
import com.chat.datatype.EnviarMensajeRequest;
import com.chat.security.TokenService;
import com.chat.enums.TipoMensaje;
import com.chat.model.Mensaje;
import com.chat.datatype.EditarMensajeRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/mensajes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MensajeController {

    @Inject
    private MensajeService mensajeService;

    @Inject
    private TokenService tokenService;

    @POST
    @Path("/enviar")
    public Response enviarMensaje(
            EnviarMensajeRequest request,
            @HeaderParam("Authorization") String token) {

        try {
            // 1. validar token
            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            Long userId = tokenService.validarToken(token);

            // 2. validar request básico
            if (request == null ||
                request.getChatId() <= 0 ||
                request.getContenido() == null || request.getContenido().isBlank() ||
                request.getTipo() == null) {

                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Datos inválidos")
                        .build();
            }

            // 3. convertir tipo
            TipoMensaje tipo;
            try {
                tipo = TipoMensaje.valueOf(request.getTipo());
            } catch (Exception e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Tipo de mensaje inválido")
                        .build();
            }

            // 4. llamar service
            mensajeService.enviarMensaje(
                    request.getChatId(),
                    userId.intValue(),
                    request.getContenido(),
                    tipo
            );

            return Response.ok("Mensaje enviado").build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();

        } catch (Exception e) {

            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error interno")
                    .build();
        }
    }

    @GET
    @Path("/chat/{chatId}")
    public Response listarMensajes(
            @PathParam("chatId") int chatId,
            @HeaderParam("Authorization") String token) {

        try {

            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            Long userId = tokenService.validarToken(token);

            boolean pertenece =
                    mensajeService.usuarioPerteneceAlChat(
                            chatId,
                            userId.intValue()
                    );

            if (!pertenece) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("No pertenece al chat")
                        .build();
            }

            return Response.ok(
                    mensajeService.listar(chatId, userId.intValue())
            ).build();

        } catch (Exception e) {

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }

    }

    @POST
    @Path("/{chatId}/leer")
    public Response marcarComoLeido(@PathParam("chatId") int chatId, @HeaderParam("Authorization") String token) {

        try {

            if (token == null || token.isBlank()) {

                return Response.status(
                    Response.Status.UNAUTHORIZED
                )
                .entity("Falta token")
                .build();
            }

            Long userId =
                tokenService.validarToken(token);

            mensajeService.marcarComoLeido(
                chatId,
                userId.intValue()
            );

            return Response.ok().build();

        } catch (Exception e) {

            return Response.status(
                Response.Status.BAD_REQUEST
            )
            .entity(e.getMessage())
            .build();
        }
    }

    @POST
    @Path("/{mensajeId}/entregado")
    public Response marcarEntregado(@PathParam("mensajeId") int mensajeId, @HeaderParam("Authorization") String token) {
        try {
            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Falta token")
                .build();
            }

            Long userId = tokenService.validarToken(token);
            mensajeService.marcarEntregado(mensajeId, userId.intValue());

            return Response.ok().build();

        } catch (Exception e) {
            return Response.status(
                Response.Status.BAD_REQUEST
            )
            .entity(e.getMessage())
            .build();
        }
    }

    @POST
    @Path("/{chatId}/leido")
    public Response marcarLeido(@PathParam("chatId") int chatId, @HeaderParam("Authorization") String token) {
        try {
            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Falta token")
                .build();
            }

            Long userId = tokenService.validarToken(token);
            mensajeService.marcarLeido(chatId, userId.intValue());

            return Response.ok().build();

        } catch (Exception e) {

            return Response.status(
                Response.Status.BAD_REQUEST
            )
            .entity(e.getMessage())
            .build();
        }
    }

    @GET
    @Path("/{mensajeId}/entregado")
    public Response fueEntregado(@PathParam("mensajeId") int mensajeId) {
        try {
            boolean entregado = mensajeService.fueEntregado(mensajeId);
            return Response.ok(entregado).build();

        } catch (Exception e) {
            return Response.status(
                Response.Status.BAD_REQUEST
            )
            .entity(e.getMessage())
            .build();
        }
    }

    @GET
    @Path("/{mensajeId}/leido")
    public Response fueLeido(@PathParam("mensajeId") int mensajeId) {
        try {
            boolean leido = mensajeService.fueLeido(mensajeId);
            return Response.ok(leido).build();

        } catch (Exception e) {
            return Response.status(
                Response.Status.BAD_REQUEST
            )
            .entity(e.getMessage())
            .build();
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarMensaje(@PathParam("id") int mensajeId,
                                EditarMensajeRequest req,
                                @HeaderParam("Authorization") String token) {

        Long usuarioId = tokenService.validarToken(token);

        Mensaje mensaje = mensajeService.editarMensaje(
            mensajeId,
            usuarioId.intValue(),
            req.getContenido()
        );

        return Response.ok(mensaje).build();
    }
}

