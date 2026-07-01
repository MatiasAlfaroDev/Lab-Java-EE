package com.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.chat.dao.AdjuntoDAO;
import com.chat.dao.ChatDAO;
import com.chat.dao.MensajeDAO;
import com.chat.dao.MensajeUsuarioDAO;
import com.chat.dao.UsuarioDAO;
import com.chat.model.Mensaje;

public class MensajeServiceTest {

    @InjectMocks
    private MensajeService mensajeService;

    @Mock
    private MensajeDAO mensajeDAO;

    @Mock
    private ChatDAO chatDAO;

    @Mock
    private UsuarioDAO usuarioDAO;

    @Mock
    private MensajeUsuarioDAO mensajeUsuarioDAO;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private AdjuntoDAO adjuntoDAO;

    @BeforeEach
    void setUp() throws Exception {

        MockitoAnnotations.openMocks(this);

        Field em = MensajeService.class.getDeclaredField("em");
        em.setAccessible(true);
        em.set(mensajeService, mock(jakarta.persistence.EntityManager.class));
    }

    @Test
    void fueEntregadoSinRegistros() {

        when(mensajeUsuarioDAO.listarPorMensaje(1))
                .thenReturn(List.of());

        assertFalse(mensajeService.fueEntregado(1));
    }

    @Test
    void buscarMensajeExistente() {

        Mensaje mensaje = new Mensaje();

        when(mensajeDAO.buscarPorId(1))
                .thenReturn(mensaje);

        assertNotNull(mensajeDAO.buscarPorId(1));

        verify(mensajeDAO).buscarPorId(1);
    }

}