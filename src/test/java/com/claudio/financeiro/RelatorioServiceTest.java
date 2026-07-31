package com.claudio.financeiro;

import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import com.claudio.financeiro.service.GastoFixoService;
import com.claudio.financeiro.service.RelatorioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private SalarioRepository salarioRepository;

    @Mock
    private GastoFixoService gastoFixoService;

    @InjectMocks
    private RelatorioService relatorioService;

    @Test
    void deveCalcularResumoComSaldoPositivo() {
        int mes = LocalDate.now().getMonthValue();
        int ano = LocalDate.now().getYear();
        when(gastoRepository.findByFiltros(1L, null, mes, ano)).thenReturn(List.of(gastoSimples(600.0)));
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioDoMesAtual(1000.0)));

        ResumoMensal resultado = relatorioService.calcularResumo(1L);

        assertValorIgual(400.0, resultado.getSaldo());
        assertEquals("Parabéns! Você economizou esse mês!", resultado.getMensagem());
    }

    @Test
    void deveCalcularResumoComSaldoNegativo() {
        int mes = LocalDate.now().getMonthValue();
        int ano = LocalDate.now().getYear();
        when(gastoRepository.findByFiltros(1L, null, mes, ano)).thenReturn(List.of(gastoSimples(1000.0)));
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioDoMesAtual(500.0)));

        ResumoMensal resultado = relatorioService.calcularResumo(1L);

        assertValorIgual(-500.0, resultado.getSaldo());
        assertEquals("Atenção! Seus gastos ultrapassaram o salário!", resultado.getMensagem());
    }

    @Test
    void deveCalcularResumoSemGastosNoMes() {
        int mes = LocalDate.now().getMonthValue();
        int ano = LocalDate.now().getYear();
        when(gastoRepository.findByFiltros(1L, null, mes, ano)).thenReturn(List.of());
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioDoMesAtual(2000.0)));

        ResumoMensal resultado = relatorioService.calcularResumo(1L);

        assertValorIgual(2000.0, resultado.getSaldo());
        assertValorIgual(0.0, resultado.getTotalGasto());
    }

    @Test
    void calcularResumoDeveIgnorarGastoFixoAindaNaoPago() {
        int mes = LocalDate.now().getMonthValue();
        int ano = LocalDate.now().getYear();
        Gasto pago = gastoSimples(600.0);
        Gasto pendente = gastoSimples(400.0);
        pendente.setPago(false);
        when(gastoRepository.findByFiltros(1L, null, mes, ano)).thenReturn(List.of(pago, pendente));
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioDoMesAtual(1000.0)));

        ResumoMensal resultado = relatorioService.calcularResumo(1L);

        assertValorIgual(400.0, resultado.getSaldo());
        assertValorIgual(600.0, resultado.getTotalGasto());
    }

    private Gasto gastoSimples(double valor) {
        Gasto gasto = new Gasto();
        gasto.setValor(BigDecimal.valueOf(valor));
        return gasto;
    }

    private Salario salarioDoMesAtual(double valor) {
        Salario salario = new Salario();
        salario.setValor(BigDecimal.valueOf(valor));
        salario.setComissao(BigDecimal.ZERO);
        salario.setAdicional(BigDecimal.ZERO);
        salario.setData(LocalDate.now());
        return salario;
    }

    private static void assertValorIgual(double esperado, BigDecimal atual) {
        assertEquals(0, BigDecimal.valueOf(esperado).compareTo(atual),
                "Esperado " + esperado + " mas recebeu " + atual);
    }
}
