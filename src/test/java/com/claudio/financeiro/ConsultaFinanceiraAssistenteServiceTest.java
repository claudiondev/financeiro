package com.claudio.financeiro;

import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Orcamento;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.OrcamentoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import com.claudio.financeiro.service.ConsultaFinanceiraAssistenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaFinanceiraAssistenteServiceTest {

    @Mock GastoRepository gastoRepository;
    @Mock SalarioRepository salarioRepository;
    @Mock OrcamentoRepository orcamentoRepository;
    @InjectMocks ConsultaFinanceiraAssistenteService service;

    private LocalDate inicio;
    private LocalDate fim;

    @BeforeEach
    void periodo() {
        inicio = LocalDate.of(2026, 1, 1);
        fim = LocalDate.of(2026, 6, 30);
    }

    @Test
    void resumeSomenteGastosPagosDoUsuarioECalculaRendaCompleta() {
        Gasto gasto = gasto("100.50", CategoriaGasto.TRANSPORTE, inicio);
        Salario salario = new Salario();
        salario.setValor(new BigDecimal("1000.00"));
        salario.setComissao(new BigDecimal("50.00"));
        when(gastoRepository.findByUsuarioIdAndDataBetweenAndPagoTrue(7L, inicio, fim)).thenReturn(List.of(gasto));
        when(salarioRepository.findByUsuarioIdAndDataBetween(7L, inicio, fim)).thenReturn(List.of(salario));

        Map<String, Object> resumo = service.resumoPeriodo(7L, inicio, fim);

        assertEquals(new BigDecimal("100.50"), resumo.get("saidasPagas"));
        assertEquals(new BigDecimal("949.50"), resumo.get("saldoRegistrado"));
        verifyNoMoreInteractions(gastoRepository, salarioRepository);
        verifyNoInteractions(orcamentoRepository);
    }

    @Test
    void somaTodosOsGastosMesmoQuandoDetalheETruncado() {
        List<Gasto> gastos = java.util.stream.IntStream.range(0, 22)
                .mapToObj(i -> gasto("10.00", CategoriaGasto.LAZER, inicio.plusDays(i))).toList();
        gastos.get(0).setDescricao("Descrição de extrato ".repeat(20));
        when(gastoRepository.findByUsuarioIdAndDataBetweenAndPagoTrue(3L, inicio, fim)).thenReturn(gastos);

        Map<String, Object> resposta = service.gastosPeriodo(3L, inicio, fim, CategoriaGasto.LAZER);

        assertEquals(new BigDecimal("220.00"), resposta.get("totalPago"));
        assertEquals(22, resposta.get("quantidade"));
        assertEquals(20, ((List<?>) resposta.get("maioresGastos")).size());
        assertEquals(true, resposta.get("detalhesLimitados"));
        Map<?, ?> primeiro = (Map<?, ?>) ((List<?>) resposta.get("maioresGastos")).get(0);
        assertTrue(((String) primeiro.get("descricao")).length() <= 81);
    }

    @Test
    void apresentaOrcamentoAtualComoReferenciaSemGerarGasto() {
        LocalDate primeiro = LocalDate.of(2026, 6, 1);
        LocalDate ultimo = LocalDate.of(2026, 6, 30);
        when(gastoRepository.findByUsuarioIdAndDataBetweenAndPagoTrue(9L, primeiro, ultimo))
                .thenReturn(List.of(gasto("120.00", CategoriaGasto.LAZER, primeiro)));
        Orcamento orcamento = new Orcamento();
        orcamento.setCategoria(CategoriaGasto.LAZER);
        orcamento.setLimiteMensal(new BigDecimal("100.00"));
        when(orcamentoRepository.findByUsuarioId(9L)).thenReturn(List.of(orcamento));

        Map<String, Object> resposta = service.orcamentos(9L, 6, 2026);

        assertEquals(true, resposta.get("limitesSaoAtuais"));
        Map<?, ?> item = (Map<?, ?>) ((List<?>) resposta.get("orcamentos")).get(0);
        assertEquals(new BigDecimal("-20.00"), item.get("restante"));
        verifyNoMoreInteractions(gastoRepository, orcamentoRepository);
    }

    @Test
    void rejeitaPeriodoMaiorQueDozeMeses() {
        assertThrows(ResponseStatusException.class,
                () -> service.gastosPeriodo(1L, inicio, inicio.plusMonths(12), null));
        verifyNoInteractions(gastoRepository);
    }

    private Gasto gasto(String valor, CategoriaGasto categoria, LocalDate data) {
        Gasto gasto = new Gasto();
        gasto.setValor(new BigDecimal(valor));
        gasto.setCategoria(categoria);
        gasto.setData(data);
        gasto.setDescricao("Compra de teste");
        gasto.setPago(true);
        return gasto;
    }
}
