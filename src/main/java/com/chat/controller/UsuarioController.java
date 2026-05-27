package com.chat.controller;

import com.chat.service.UsuarioService;
import com.chat.model.Usuario;
import com.chat.security.TokenService;
import com.chat.datatype.LoginRequest;
import com.chat.datatype.UsuarioDTO;
import com.chat.datatype.LoginResponse;

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

    @Inject
    private TokenService tokenService;

    // REGISTRO
    @POST
    @Path("/registro")
    public Response registrarUsuario(Usuario request) {
        try {
            if ( 
                request == null || 
                request.getNombre() == null || 
                request.getNombre().isBlank() || 
                request.getEmail() == null || 
                request.getEmail().isBlank() || 
                request.getPassword() == null || 
                request.getPassword().isBlank() 
            ) { 
                return Response.status(Response.Status.BAD_REQUEST)
                 .entity("Datos inválidos") 
                 .build(); }

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

            String token = tokenService.generarToken(usuario);

            return Response.ok(new LoginResponse(token, dto)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity(e.getMessage())
                           .build();
        }
    }

    // LOGOUT
    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Authorization") String token) {

        try {
            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            Long userId =
                tokenService.validarToken(token);

            usuarioService.logout(
                userId.intValue()
            );

            tokenService.eliminarToken(token);

            return Response.ok("Logout exitoso").build();

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token inválido")
                    .build();
        }
    }

    @GET
    @Path("/listar")
    public Response listarUsuarios(@HeaderParam("Authorization") String token) {

        try {

            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            tokenService.validarToken(token);

            var usuarios = usuarioService.listarUsuarios();

            return Response.ok(usuarios).build();

        } catch (Exception e) {

            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token inválido")
                    .build();
        }
    }

}