package com.claudio.financeiro;

import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.service.GastoService;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GastoServiceTest {

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private SalarioRepository salarioRepository;

    @InjectMocks
    private GastoService gastoService;

    @Test
    void deveCalcularResumoComSaldoPositivo() {
        // 1. DADOS FALSOS
        Gasto gasto = new Gasto();
        gasto.setValor(600.0);

        Salario salario = new Salario();
        salario.setValor(1000.0);
        salario.setComissao(0.0);
        salario.setAdicional(0.0);

        // 2. ENSINANDO OS MOCKS O QUE RETORNAR
        when(gastoRepository.findByUsuarioId(1L))
                .thenReturn(List.of(gasto));
        when(salarioRepository.findByUsuarioId(1L))
                .thenReturn(List.of(salario));

        // 3. EXECUTANDO O MÉTODO
        ResumoMensal resultado = gastoService.calcularResumo(1L);

        // 4. VERIFICANDO O RESULTADO
        assertEquals(400.0, resultado.getSaldo());
        assertEquals("Parabéns! Você economizou esse mês!", resultado.getMensagem());

    }

    @Test
    void deveCalcularResumoComSaldoNegativo() {
        // 1. DADOS FALSOS
        Gasto gasto = new Gasto();
        gasto.setValor(1000.0);

        Salario salario = new Salario();
        salario.setValor(500.0);
        salario.setComissao(0.0);
        salario.setAdicional(0.0);

        // 2. ENSINANDO OS MOCKS O QUE RETORNAR
        when(gastoRepository.findByUsuarioId(1L))
                .thenReturn(List.of(gasto));
        when(salarioRepository.findByUsuarioId(1L))
                .thenReturn(List.of(salario));

        ResumoMensal resultado = gastoService.calcularResumo(1L);

        assertEquals(-500.0, resultado.getSaldo());
        assertEquals("Atenção! Seus gastos ultrapassaram o salário!", resultado.getMensagem());
    }

}
