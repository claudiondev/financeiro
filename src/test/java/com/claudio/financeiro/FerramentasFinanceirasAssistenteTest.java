package com.claudio.financeiro;

import com.claudio.financeiro.service.ConsultaFinanceiraAssistenteService;
import com.claudio.financeiro.service.FerramentasFinanceirasAssistente;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FerramentasFinanceirasAssistenteTest {

    @Test
    void usuarioVemDoContextoEQuartaChamadaERecusada() {
        ConsultaFinanceiraAssistenteService consultas = mock(ConsultaFinanceiraAssistenteService.class);
        FerramentasFinanceirasAssistente ferramentas = new FerramentasFinanceirasAssistente(consultas);
        ToolContext contexto = new ToolContext(Map.of("usuarioId", 42L, "chamadas", new AtomicInteger()));

        for (int i = 0; i < 3; i++) ferramentas.consultarOrcamentos(6, 2026, contexto);
        assertThrows(ResponseStatusException.class,
                () -> ferramentas.consultarOrcamentos(6, 2026, contexto));
        verify(consultas, times(3)).orcamentos(42L, 6, 2026);
        verifyNoMoreInteractions(consultas);
    }
}
