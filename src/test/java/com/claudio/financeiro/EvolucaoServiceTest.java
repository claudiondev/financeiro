package com.claudio.financeiro;

import com.claudio.financeiro.dto.EvolucaoMensalDTO;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import com.claudio.financeiro.service.EvolucaoService;
import com.claudio.financeiro.service.GastoFixoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvolucaoServiceTest {

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private SalarioRepository salarioRepository;

    @Mock
    private GastoFixoService gastoFixoService;

    @InjectMocks
    private EvolucaoService evolucaoService;

    @Test
    void deveRetornarEvolucaoDosUltimosNMeses() {
        YearMonth mesAtual = YearMonth.now();
        YearMonth mesAnterior = mesAtual.minusMonths(1);

        when(gastoRepository.somarGastosPagosAgrupadoPorMes(eq(1L), any())).thenReturn(List.of(
                new Object[]{mesAnterior.getYear(), mesAnterior.getMonthValue(), BigDecimal.valueOf(100.0)},
                new Object[]{mesAtual.getYear(), mesAtual.getMonthValue(), BigDecimal.valueOf(50.0)}
        ));
        when(salarioRepository.somarRendaAgrupadaPorMes(eq(1L), any())).thenReturn(List.of());

        List<EvolucaoMensalDTO> resultado = evolucaoService.getEvolucaoMensal(1L, 2);

        assertEquals(2, resultado.size());
        assertValorIgual(100.0, resultado.get(0).getTotalSaidas());
        assertValorIgual(50.0, resultado.get(1).getTotalSaidas());
    }

    @Test
    void deveOrdenarEvolucaoCronologicamente() {
        when(gastoRepository.somarGastosPagosAgrupadoPorMes(eq(1L), any())).thenReturn(List.of());
        when(salarioRepository.somarRendaAgrupadaPorMes(eq(1L), any())).thenReturn(List.of());

        List<EvolucaoMensalDTO> resultado = evolucaoService.getEvolucaoMensal(1L, 3);

        YearMonth esperado = YearMonth.now().minusMonths(2);
        for (EvolucaoMensalDTO ponto : resultado) {
            assertEquals(esperado.getMonthValue(), ponto.getMes());
            assertEquals(esperado.getYear(), ponto.getAno());
            esperado = esperado.plusMonths(1);
        }
    }

    @Test
    void deveRetornarZeroParaMesesSemRegistros() {
        when(gastoRepository.somarGastosPagosAgrupadoPorMes(eq(1L), any())).thenReturn(List.of());
        when(salarioRepository.somarRendaAgrupadaPorMes(eq(1L), any())).thenReturn(List.of());

        List<EvolucaoMensalDTO> resultado = evolucaoService.getEvolucaoMensal(1L, 1);

        assertValorIgual(0.0, resultado.get(0).getTotalSaidas());
        assertValorIgual(0.0, resultado.get(0).getTotalEntradas());
        assertValorIgual(0.0, resultado.get(0).getSaldo());
    }

    private static void assertValorIgual(double esperado, BigDecimal atual) {
        assertEquals(0, BigDecimal.valueOf(esperado).compareTo(atual),
                "Esperado " + esperado + " mas recebeu " + atual);
    }
}
