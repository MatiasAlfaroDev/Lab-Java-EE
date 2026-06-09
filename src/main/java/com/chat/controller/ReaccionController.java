package com.chat.controller;

import com.chat.datatype.ReaccionRequest;
import com.chat.security.TokenService;
import com.chat.service.ReaccionService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/reacciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReaccionController {

    @Inject
    private ReaccionService reaccionService;

    @Inject
    private TokenService tokenService;

    @POST
    public Response reaccionar(
            ReaccionRequest request,
            @HeaderParam("Authorization") String token
    ) {

        try {

            if (token == null || token.isBlank()) {

                return Response.status(
                        Response.Status.UNAUTHORIZED
                )
                .entity("Falta token")
                .build();
            }

            if (request == null ||
            request.getMensajeId() <= 0 ||
            request.getEmoji() == null ||
            request.getEmoji().isBlank()) {

            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Datos inválidos")
                .build();
            }

            Long userId =
                    tokenService.validarToken(token);

            reaccionService.reaccionar(
                    request.getMensajeId(),
                    userId.intValue(),
                    request.getEmoji()
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
}