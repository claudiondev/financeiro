package com.claudio.financeiro;

import com.claudio.financeiro.dto.CriarGastoRequest;
import com.claudio.financeiro.dto.EvolucaoMensalDTO;
import com.claudio.financeiro.dto.GastoDTO;
import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import com.claudio.financeiro.service.GastoFixoService;
import com.claudio.financeiro.service.GastoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GastoServiceTest {

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private SalarioRepository salarioRepository;

    @Mock
    private GastoFixoService gastoFixoService;

    @InjectMocks
    private GastoService gastoService;

    @Test
    void deveSalvarGastoERetornarORegistro() {
        Gasto gasto = gastoComUsuario(1L);
        when(gastoRepository.save(gasto)).thenReturn(gasto);

        Gasto resultado = gastoService.salvar(gasto);

        assertEquals(gasto, resultado);
        verify(gastoRepository).save(gasto);
    }

    @Test
    void deveCriarGastoAPartirDoRequest() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        CriarGastoRequest request = new CriarGastoRequest(
                "Mercado", BigDecimal.valueOf(250.0), CategoriaGasto.ALIMENTACAO, LocalDate.of(2026, 7, 10)
        );
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        GastoDTO resultado = gastoService.criar(request, usuario);

        assertEquals("Mercado", resultado.getDescricao());
        assertEquals(CategoriaGasto.ALIMENTACAO, resultado.getCategoria());
        assertEquals(1L, resultado.getUsuarioId());
        assertValorIgual(250.0, resultado.getValor());
    }

    @Test
    void deveAtualizarGastoQuandoUsuarioEhDono() {
        Gasto gastoExistente = gastoComUsuario(1L);
        when(gastoRepository.findById(10L)).thenReturn(Optional.of(gastoExistente));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CriarGastoRequest request = new CriarGastoRequest(
                "Mercado atualizado", BigDecimal.valueOf(180.0), CategoriaGasto.ALIMENTACAO, LocalDate.of(2026, 7, 15)
        );

        GastoDTO resultado = gastoService.atualizar(10L, request, 1L);

        assertEquals("Mercado atualizado", resultado.getDescricao());
        assertEquals(CategoriaGasto.ALIMENTACAO, resultado.getCategoria());
        assertValorIgual(180.0, resultado.getValor());
        verify(gastoRepository).save(gastoExistente);
    }

    @Test
    void deveLancarForbiddenAoAtualizarGastoDeOutroUsuario() {
        Gasto gasto = gastoComUsuario(1L);
        when(gastoRepository.findById(10L)).thenReturn(Optional.of(gasto));
        CriarGastoRequest request = new CriarGastoRequest("X", BigDecimal.TEN, CategoriaGasto.OUTROS, LocalDate.now());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gastoService.atualizar(10L, request, 2L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(gastoRepository, never()).save(any());
    }

    @Test
    void deveLancarNotFoundAoAtualizarGastoInexistente() {
        when(gastoRepository.findById(99L)).thenReturn(Optional.empty());
        CriarGastoRequest request = new CriarGastoRequest("X", BigDecimal.TEN, CategoriaGasto.OUTROS, LocalDate.now());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gastoService.atualizar(99L, request, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(gastoRepository, never()).save(any());
    }

    @Test
    void deveListarGastosFiltradosPeloUsuario() {
        Gasto g1 = gastoComValor(1L, "Aluguel", 1500.0);
        Gasto g2 = gastoComValor(1L, "Mercado", 300.0);
        when(gastoRepository.findByUsuarioId(1L)).thenReturn(List.of(g1, g2));

        List<GastoDTO> resultado = gastoService.listarPorUsuario(1L);

        assertEquals(2, resultado.size());
        assertEquals("Aluguel", resultado.get(0).getDescricao());
        assertValorIgual(300.0, resultado.get(1).getValor());
        verify(gastoRepository).findByUsuarioId(1L);
    }

    @Test
    void deveDeletarGastoQuandoUsuarioEhODono() {
        Gasto gasto = gastoComUsuario(1L);
        when(gastoRepository.findById(10L)).thenReturn(Optional.of(gasto));

        gastoService.deletar(10L, 1L);

        verify(gastoRepository).deleteById(10L);
    }

    // IDOR: usuário 2 tenta deletar gasto do usuário 1
    @Test
    void deveLancarForbiddenAoDeletarGastoDeOutroUsuario() {
        Gasto gasto = gastoComUsuario(1L);
        when(gastoRepository.findById(10L)).thenReturn(Optional.of(gasto));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gastoService.deletar(10L, 2L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(gastoRepository, never()).deleteById(any());
    }

    @Test
    void deveLancarNotFoundAoDeletarGastoInexistente() {
        when(gastoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gastoService.deletar(99L, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(gastoRepository, never()).deleteById(any());
    }

    @Test
    void deveCalcularResumoComSaldoPositivo() {
        when(gastoRepository.findByUsuarioId(1L)).thenReturn(List.of(gastoSimples(600.0)));
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioSimples(1000.0)));

        ResumoMensal resultado = gastoService.calcularResumo(1L);

        assertValorIgual(400.0, resultado.getSaldo());
        assertEquals("Parabéns! Você economizou esse mês!", resultado.getMensagem());
    }

    @Test
    void deveCalcularResumoComSaldoNegativo() {
        when(gastoRepository.findByUsuarioId(1L)).thenReturn(List.of(gastoSimples(1000.0)));
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioSimples(500.0)));

        ResumoMensal resultado = gastoService.calcularResumo(1L);

        assertValorIgual(-500.0, resultado.getSaldo());
        assertEquals("Atenção! Seus gastos ultrapassaram o salário!", resultado.getMensagem());
    }

    @Test
    void deveCalcularResumoSemGastosNoMes() {
        when(gastoRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioSimples(2000.0)));

        ResumoMensal resultado = gastoService.calcularResumo(1L);

        assertValorIgual(2000.0, resultado.getSaldo());
        assertValorIgual(0.0, resultado.getTotalGasto());
    }

    @Test
    void calcularResumoDeveIgnorarGastoFixoAindaNaoPago() {
        Gasto pago = gastoSimples(600.0);
        Gasto pendente = gastoSimples(400.0);
        pendente.setPago(false);
        when(gastoRepository.findByUsuarioId(1L)).thenReturn(List.of(pago, pendente));
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioSimples(1000.0)));

        ResumoMensal resultado = gastoService.calcularResumo(1L);

        // Só os 600 pagos entram no saldo — os 400 pendentes ficam de fora até serem confirmados
        assertValorIgual(400.0, resultado.getSaldo());
        assertValorIgual(600.0, resultado.getTotalGasto());
    }

    @Test
    void deveRetornarEvolucaoDosUltimosNMeses() {
        YearMonth mesAtual = YearMonth.now();
        YearMonth mesAnterior = mesAtual.minusMonths(1);

        when(gastoRepository.findByFiltros(1L, null, mesAnterior.getMonthValue(), mesAnterior.getYear()))
                .thenReturn(List.of(gastoSimples(100.0)));
        when(gastoRepository.findByFiltros(1L, null, mesAtual.getMonthValue(), mesAtual.getYear()))
                .thenReturn(List.of(gastoSimples(50.0)));
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of());

        List<EvolucaoMensalDTO> resultado = gastoService.getEvolucaoMensal(1L, 2);

        assertEquals(2, resultado.size());
        assertValorIgual(100.0, resultado.get(0).getTotalSaidas());
        assertValorIgual(50.0, resultado.get(1).getTotalSaidas());
    }

    @Test
    void deveOrdenarEvolucaoCronologicamente() {
        when(gastoRepository.findByFiltros(eq(1L), isNull(), anyInt(), anyInt())).thenReturn(List.of());
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of());

        List<EvolucaoMensalDTO> resultado = gastoService.getEvolucaoMensal(1L, 3);

        YearMonth esperado = YearMonth.now().minusMonths(2);
        for (EvolucaoMensalDTO ponto : resultado) {
            assertEquals(esperado.getMonthValue(), ponto.getMes());
            assertEquals(esperado.getYear(), ponto.getAno());
            esperado = esperado.plusMonths(1);
        }
    }

    @Test
    void deveRetornarZeroParaMesesSemRegistros() {
        when(gastoRepository.findByFiltros(eq(1L), isNull(), anyInt(), anyInt())).thenReturn(List.of());
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of());

        List<EvolucaoMensalDTO> resultado = gastoService.getEvolucaoMensal(1L, 1);

        assertValorIgual(0.0, resultado.get(0).getTotalSaidas());
        assertValorIgual(0.0, resultado.get(0).getTotalEntradas());
        assertValorIgual(0.0, resultado.get(0).getSaldo());
    }

    private Gasto gastoComUsuario(Long usuarioId) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Gasto gasto = new Gasto();
        gasto.setDescricao("Teste");
        gasto.setValor(BigDecimal.valueOf(100.0));
        gasto.setCategoria(CategoriaGasto.OUTROS);
        gasto.setData(LocalDate.now());
        gasto.setUsuario(usuario);
        return gasto;
    }

    private Gasto gastoComValor(Long usuarioId, String descricao, double valor) {
        Gasto gasto = gastoComUsuario(usuarioId);
        gasto.setDescricao(descricao);
        gasto.setValor(BigDecimal.valueOf(valor));
        return gasto;
    }

    private Gasto gastoSimples(double valor) {
        Gasto gasto = new Gasto();
        gasto.setValor(BigDecimal.valueOf(valor));
        return gasto;
    }

    private Salario salarioSimples(double valor) {
        Salario salario = new Salario();
        salario.setValor(BigDecimal.valueOf(valor));
        salario.setComissao(BigDecimal.ZERO);
        salario.setAdicional(BigDecimal.ZERO);
        return salario;
    }

    // BigDecimal.equals() é sensível à escala ("400.0" != "400.00") — compareTo é a forma
    // correta de comparar valor numérico independente de como o BigDecimal foi construído.
    private static void assertValorIgual(double esperado, BigDecimal atual) {
        assertEquals(0, BigDecimal.valueOf(esperado).compareTo(atual),
                () -> "Esperado " + esperado + " mas foi " + atual);
    }
}
