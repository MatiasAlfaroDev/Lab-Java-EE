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

import com.chat.dao.ChatDAO;
import com.chat.dao.UsuarioDAO;
import com.chat.dao.MensajeDAO;
import com.chat.enums.TipoChat;
import com.chat.model.Chat;

public class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private ChatDAO chatDAO;

    @Mock
    private UsuarioDAO usuarioDAO;

    @Mock
    private MensajeDAO mensajeDAO;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        Field em = ChatService.class.getDeclaredField("em");
        em.setAccessible(true);
        em.set(chatService, mock(jakarta.persistence.EntityManager.class));
    }

    @Test
    void obtenerChats_devuelveLista() {

        Chat chat = new Chat();
        chat.setNombre("General");
        chat.setTipo(TipoChat.GRUPO);

        when(chatDAO.obtenerTodosChats())
                .thenReturn(List.of(chat));

        List<Chat> resultado = chatService.obtenerChats();

        assertEquals(1, resultado.size());
        assertEquals("General", resultado.get(0).getNombre());

        verify(chatDAO).obtenerTodosChats();
    }

    @Test
    void obtenerChatsSinDatos() {

        when(chatDAO.obtenerTodosChats())
                .thenReturn(List.of());

        List<Chat> resultado = chatService.obtenerChats();

        assertTrue(resultado.isEmpty());
    }

}