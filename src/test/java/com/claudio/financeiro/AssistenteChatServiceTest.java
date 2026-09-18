package com.claudio.financeiro;

import com.claudio.financeiro.dto.MensagemChatRequest;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.AssistenteChatService;
import com.claudio.financeiro.service.FerramentasFinanceirasAssistente;
import com.claudio.financeiro.service.QuotaAssistenteService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssistenteChatServiceTest {

    @Test
    void semChaveDesligaChatSemAfetarServico() {
        @SuppressWarnings("unchecked") ObjectProvider<ChatClient> provider = mock(ObjectProvider.class);
        AssistenteChatService service = new AssistenteChatService(provider,
                mock(FerramentasFinanceirasAssistente.class), mock(QuotaAssistenteService.class));
        assertFalse(service.habilitado());
        assertThrows(ResponseStatusException.class, () -> service.criarSessao(1L));
    }

    @Test
    void doisVisitantesDaMesmaContaDemoNaoCompartilhamSessao() {
        @SuppressWarnings("unchecked") ObjectProvider<ChatClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mock(ChatClient.class));
        AssistenteChatService service = new AssistenteChatService(provider,
                mock(FerramentasFinanceirasAssistente.class), mock(QuotaAssistenteService.class));

        String primeira = service.criarSessao(1L);
        String segunda = service.criarSessao(1L);
        assertNotEquals(primeira, segunda);
        service.encerrarSessao(primeira, 1L);
        assertThrows(ResponseStatusException.class,
                () -> service.responder(primeira, usuario(1L), new MensagemChatRequest("teste", UUID.randomUUID())));
        assertThrows(ResponseStatusException.class, () -> service.encerrarSessao(segunda, 2L));
        service.encerrarSessao(segunda, 1L);
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setDemo(true);
        return usuario;
    }
}
