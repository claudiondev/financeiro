package com.claudio.financeiro;

import com.claudio.financeiro.dto.CriarGastoRequest;
import com.claudio.financeiro.dto.CriarGastoResponse;
import com.claudio.financeiro.dto.GastoDTO;
import com.claudio.financeiro.dto.InsightDTO;
import com.claudio.financeiro.dto.ParcelamentoDTO;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.FormaPagamento;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Severidade;
import com.claudio.financeiro.model.TipoInsight;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.service.GastoFixoService;
import com.claudio.financeiro.service.GastoService;
import com.claudio.financeiro.service.OrcamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private GastoFixoService gastoFixoService;

    @Mock
    private OrcamentoService orcamentoService;

    @InjectMocks
    private GastoService gastoService;

    @Test
    void deveCriarGastoAPartirDoRequest() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        CriarGastoRequest request = requestSimples(
                "Mercado", BigDecimal.valueOf(250.0), CategoriaGasto.ALIMENTACAO, LocalDate.of(2026, 7, 10)
        );
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        GastoDTO resultado = gastoService.criar(request, usuario).getGasto();

        assertEquals("Mercado", resultado.getDescricao());
        assertEquals(CategoriaGasto.ALIMENTACAO, resultado.getCategoria());
        assertEquals(1L, resultado.getUsuarioId());
        assertValorIgual(250.0, resultado.getValor());
    }

    @Test
    void deveIncluirAvisoDeOrcamentoQuandoGastoEstouraCategoria() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        CriarGastoRequest request = requestSimples(
                "Mercado", BigDecimal.valueOf(250.0), CategoriaGasto.ALIMENTACAO, LocalDate.of(2026, 7, 10)
        );
        InsightDTO aviso = new InsightDTO(TipoInsight.ORCAMENTO_ESTOURADO, Severidade.CRITICO,
                CategoriaGasto.ALIMENTACAO, "Orçamento estourado", "mensagem");
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(orcamentoService.avaliarAvisoDeEstouro(1L, CategoriaGasto.ALIMENTACAO, LocalDate.of(2026, 7, 10)))
                .thenReturn(aviso);

        CriarGastoResponse resposta = gastoService.criar(request, usuario);

        assertEquals(aviso, resposta.getAvisoOrcamento());
    }

    @Test
    void deveAtualizarGastoQuandoUsuarioEhDono() {
        Gasto gastoExistente = gastoComUsuario(1L);
        when(gastoRepository.findById(10L)).thenReturn(Optional.of(gastoExistente));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CriarGastoRequest request = requestSimples(
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
        CriarGastoRequest request = requestSimples("X", BigDecimal.TEN, CategoriaGasto.OUTROS, LocalDate.now());

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
        CriarGastoRequest request = requestSimples("X", BigDecimal.TEN, CategoriaGasto.OUTROS, LocalDate.now());

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
    void deveCriarUmaParcelaPorMesEmCompraParcelada() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        CriarGastoRequest request = requestSimples(
                "Notebook", BigDecimal.valueOf(500.0), CategoriaGasto.OUTROS, LocalDate.of(2026, 7, 10)
        );
        request.setFormaPagamento(FormaPagamento.CARTAO_CREDITO);
        request.setTotalParcelas(5);
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> inv.getArgument(0));

        gastoService.criar(request, usuario);

        ArgumentCaptor<Gasto> captor = ArgumentCaptor.forClass(Gasto.class);
        verify(gastoRepository, times(5)).save(captor.capture());
        List<Gasto> parcelas = captor.getAllValues();

        assertEquals(LocalDate.of(2026, 7, 10), parcelas.get(0).getData());
        assertEquals(LocalDate.of(2026, 11, 10), parcelas.get(4).getData());
        assertEquals(1, parcelas.get(0).getNumeroParcela());
        assertEquals(5, parcelas.get(0).getTotalParcelas());
        // Todas as parcelas compartilham o mesmo grupo, e nascem pagas (débito automático na fatura)
        assertEquals(parcelas.get(0).getGrupoParcelamento(), parcelas.get(4).getGrupoParcelamento());
        assertTrue(parcelas.get(0).isPago());
    }

    @Test
    void somaDasParcelasDeveBaterExatamenteComOValorDaCompra() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        CriarGastoRequest request = requestSimples(
                "TV", BigDecimal.valueOf(500.0), CategoriaGasto.OUTROS, LocalDate.of(2026, 7, 10)
        );
        request.setFormaPagamento(FormaPagamento.CARTAO_CREDITO);
        request.setTotalParcelas(3);
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> inv.getArgument(0));

        gastoService.criar(request, usuario);

        ArgumentCaptor<Gasto> captor = ArgumentCaptor.forClass(Gasto.class);
        verify(gastoRepository, times(3)).save(captor.capture());

        // 500/3 não é exato: 166,67 + 166,67 + 166,66 — a última absorve a diferença
        assertValorIgual(166.67, captor.getAllValues().get(0).getValor());
        assertValorIgual(166.66, captor.getAllValues().get(2).getValor());
        BigDecimal soma = captor.getAllValues().stream()
                .map(Gasto::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertValorIgual(500.0, soma);
    }

    @Test
    void deveRejeitarParcelamentoQuandoFormaDePagamentoNaoEhCredito() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        CriarGastoRequest request = requestSimples(
                "Mercado", BigDecimal.valueOf(300.0), CategoriaGasto.ALIMENTACAO, LocalDate.now()
        );
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setTotalParcelas(3);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gastoService.criar(request, usuario)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(gastoRepository, never()).save(any());
    }

    @Test
    void deveCriarGastoUnicoQuandoTotalParcelasEhUm() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        CriarGastoRequest request = requestSimples(
                "Uber", BigDecimal.valueOf(32.5), CategoriaGasto.TRANSPORTE, LocalDate.now()
        );
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setTotalParcelas(1);
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> inv.getArgument(0));

        GastoDTO resultado = gastoService.criar(request, usuario).getGasto();

        verify(gastoRepository, times(1)).save(any());
        assertNull(resultado.getNumeroParcela());
    }

    @Test
    void deveListarParcelamentoEmAbertoComValorRestante() {
        YearMonth mesAtual = YearMonth.now();
        // 3 parcelas: uma já venceu (mês passado), duas ainda a vencer (este mês e o próximo)
        List<Gasto> parcelas = List.of(
                parcelaDeCompra("grupo-1", 1, 3, 100.0, mesAtual.minusMonths(1).atDay(10)),
                parcelaDeCompra("grupo-1", 2, 3, 100.0, mesAtual.atDay(10)),
                parcelaDeCompra("grupo-1", 3, 3, 100.0, mesAtual.plusMonths(1).atDay(10))
        );
        when(gastoRepository.findByUsuarioIdAndGrupoParcelamentoIsNotNull(1L)).thenReturn(parcelas);

        List<ParcelamentoDTO> resultado = gastoService.listarParcelamentosEmAberto(1L);

        assertEquals(1, resultado.size());
        assertValorIgual(300.0, resultado.get(0).getValorTotal());
        assertValorIgual(200.0, resultado.get(0).getValorRestante());
        assertEquals(1, resultado.get(0).getParcelasPagas());
        assertEquals(mesAtual.plusMonths(1).atDay(10), resultado.get(0).getUltimaParcela());
    }

    @Test
    void naoDeveListarParcelamentoJaQuitado() {
        YearMonth mesAtual = YearMonth.now();
        List<Gasto> parcelas = List.of(
                parcelaDeCompra("grupo-1", 1, 2, 100.0, mesAtual.minusMonths(2).atDay(10)),
                parcelaDeCompra("grupo-1", 2, 2, 100.0, mesAtual.minusMonths(1).atDay(10))
        );
        when(gastoRepository.findByUsuarioIdAndGrupoParcelamentoIsNotNull(1L)).thenReturn(parcelas);

        List<ParcelamentoDTO> resultado = gastoService.listarParcelamentosEmAberto(1L);

        assertTrue(resultado.isEmpty());
    }

    private CriarGastoRequest requestSimples(String descricao, BigDecimal valor, CategoriaGasto categoria, LocalDate data) {
        return new CriarGastoRequest(descricao, valor, categoria, data, null, null);
    }

    private Gasto parcelaDeCompra(String grupo, int numero, int total, double valor, LocalDate data) {
        Gasto gasto = new Gasto();
        gasto.setDescricao("Notebook");
        gasto.setCategoria(CategoriaGasto.OUTROS);
        gasto.setValor(BigDecimal.valueOf(valor));
        gasto.setData(data);
        gasto.setGrupoParcelamento(grupo);
        gasto.setNumeroParcela(numero);
        gasto.setTotalParcelas(total);
        return gasto;
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

    // BigDecimal.equals() é sensível à escala ("400.0" != "400.00") — compareTo é a forma
    // correta de comparar valor numérico independente de como o BigDecimal foi construído.
    private static void assertValorIgual(double esperado, BigDecimal atual) {
        assertEquals(0, BigDecimal.valueOf(esperado).compareTo(atual),
                () -> "Esperado " + esperado + " mas foi " + atual);
    }
}
