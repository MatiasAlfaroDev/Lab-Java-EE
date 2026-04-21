package com.chat.controller;

import com.chat.service.UsuarioService;
import com.chat.model.Usuario;
import com.chat.datatype.LoginRequest;
import com.chat.datatype.UsuarioDTO;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioController {

    @Inject
    private UsuarioService usuarioService;

    // REGISTRO
    @POST
    @Path("/registro")
    public Response registrarUsuario(Usuario request) {
        try {
            usuarioService.registrarUsuario(
                request.getNombre(),
                request.getEmail(),
                request.getPassword(),
                request.getRol()
            );

            return Response.ok("Usuario registrado").build();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(e.getMessage())
                           .build();
        }
    }

    // LOGIN
    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            Usuario usuario = usuarioService.login(
                request.getEmail(),
                request.getPassword()
            );

            UsuarioDTO dto = new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getEstado().name()
            );

            return Response.ok(dto).build();

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity(e.getMessage())
                           .build();
        }
    }
}