package com.claudio.financeiro;

import com.claudio.financeiro.dto.MensagemChatRequest;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.AssistenteChatService;
import com.claudio.financeiro.service.FerramentasFinanceirasAssistente;
import com.claudio.financeiro.service.QuotaAssistenteService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;

import org.mockito.ArgumentCaptor;
import org.mockito.Answers;

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

    @Test
    void reenvioDaMesmaPerguntaNaoChamaProvedorNemConsomeQuotaOutraVez() {
        @SuppressWarnings("unchecked") ObjectProvider<ChatClient> provider = mock(ObjectProvider.class);
        ChatClient cliente = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec chamada = mock(ChatClient.ChatClientRequestSpec.class, Answers.RETURNS_SELF);
        ChatClient.CallResponseSpec retorno = mock(ChatClient.CallResponseSpec.class);
        when(provider.getIfAvailable()).thenReturn(cliente);
        when(cliente.prompt()).thenReturn(chamada);
        when(chamada.call()).thenReturn(retorno);
        when(retorno.content()).thenReturn("Você gastou R$ 25,00.");
        QuotaAssistenteService quota = mock(QuotaAssistenteService.class);
        AssistenteChatService service = new AssistenteChatService(provider,
                mock(FerramentasFinanceirasAssistente.class), quota);
        String sessaoA = service.criarSessao(1L);
        String sessaoB = service.criarSessao(1L);
        MensagemChatRequest pergunta = new MensagemChatRequest("Quanto gastei?", UUID.randomUUID());

        assertEquals("Você gastou R$ 25,00.", service.responder(sessaoA, usuario(1L), pergunta).resposta());
        assertEquals("Você gastou R$ 25,00.", service.responder(sessaoA, usuario(1L), pergunta).resposta());
        service.responder(sessaoB, usuario(1L), new MensagemChatRequest("Outra pergunta", UUID.randomUUID()));

        verify(quota, times(2)).reservar(1L, true);
        verify(cliente, times(2)).prompt();
        ArgumentCaptor<List<Message>> historicos = ArgumentCaptor.forClass(List.class);
        verify(chamada, times(2)).messages(historicos.capture());
        assertTrue(historicos.getAllValues().stream().allMatch(List::isEmpty));
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setDemo(true);
        return usuario;
    }
}
