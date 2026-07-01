package com.chat.service;

import com.chat.dao.UsuarioDAO;
import com.chat.enums.TipoEstado;
import com.chat.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioDAO usuarioDAO;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void setup() {
    }

    @Test
    void registrarUsuarioCorrectamente() {

        when(usuarioDAO.existeEmail("juan@test.com"))
                .thenReturn(false);

        usuarioService.registrarUsuario(
                "Juan",
                "juan@test.com",
                "Password1!",
                "USER"
        );

        verify(usuarioDAO, times(1))
                .guardar(any(Usuario.class));
    }

    @Test
    void registrarUsuarioEmailDuplicado() {

        when(usuarioDAO.existeEmail("juan@test.com"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.registrarUsuario(
                        "Juan",
                        "juan@test.com",
                        "Password1!",
                        "USER")
        );
    }

    @Test
    void loginCorrecto() {

        Usuario usuario = new Usuario();

        usuario.setNombre("Juan");
        usuario.setEmail("juan@test.com");
        usuario.setPassword(
                org.mindrot.jbcrypt.BCrypt.hashpw(
                        "Password1!",
                        org.mindrot.jbcrypt.BCrypt.gensalt()
                )
        );

        usuario.setEstado(TipoEstado.OFFLINE);
        usuario.setRol("USER");

        when(usuarioDAO.buscarEmail("juan@test.com"))
                .thenReturn(usuario);

        Usuario resultado = usuarioService.login(
                "juan@test.com",
                "Password1!"
        );

        assertNotNull(resultado);

        verify(usuarioDAO).actualizar(usuario);
    }

    @Test
    void loginPasswordIncorrecta() {

        Usuario usuario = new Usuario();

        usuario.setPassword(
                org.mindrot.jbcrypt.BCrypt.hashpw(
                        "Password1!",
                        org.mindrot.jbcrypt.BCrypt.gensalt()
                )
        );

        when(usuarioDAO.buscarEmail(anyString()))
                .thenReturn(usuario);

        assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.login(
                        "juan@test.com",
                        "otraPassword")
        );
    }

}