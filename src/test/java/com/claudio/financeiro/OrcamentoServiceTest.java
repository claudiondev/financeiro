package com.claudio.financeiro;

import com.claudio.financeiro.dto.CriarOrcamentoRequest;
import com.claudio.financeiro.dto.OrcamentoDTO;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Orcamento;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.OrcamentoRepository;
import com.claudio.financeiro.service.OrcamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private GastoRepository gastoRepository;

    @InjectMocks
    private OrcamentoService orcamentoService;

    @Test
    void deveCriarNovoOrcamentoQuandoNaoExiste() {
        Usuario usuario = usuarioComId(1L);
        when(orcamentoRepository.findByUsuarioIdAndCategoria(1L, CategoriaGasto.TRANSPORTE))
                .thenReturn(Optional.empty());
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));

        CriarOrcamentoRequest request = new CriarOrcamentoRequest(CategoriaGasto.TRANSPORTE, BigDecimal.valueOf(400.0));
        OrcamentoDTO resultado = orcamentoService.salvarOuAtualizar(request, usuario);

        assertEquals(CategoriaGasto.TRANSPORTE, resultado.getCategoria());
        assertEquals(0, BigDecimal.valueOf(400.0).compareTo(resultado.getLimiteMensal()));
        verify(orcamentoRepository).findByUsuarioIdAndCategoria(1L, CategoriaGasto.TRANSPORTE);
        verify(orcamentoRepository).save(any(Orcamento.class));
    }

    @Test
    void deveAtualizarOrcamentoExistenteAoInvesDeDuplicar() {
        Usuario usuario = usuarioComId(1L);
        Orcamento existente = new Orcamento();
        existente.setId(5L);
        existente.setCategoria(CategoriaGasto.TRANSPORTE);
        existente.setLimiteMensal(BigDecimal.valueOf(300.0));
        existente.setUsuario(usuario);

        when(orcamentoRepository.findByUsuarioIdAndCategoria(1L, CategoriaGasto.TRANSPORTE))
                .thenReturn(Optional.of(existente));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));

        CriarOrcamentoRequest request = new CriarOrcamentoRequest(CategoriaGasto.TRANSPORTE, BigDecimal.valueOf(450.0));
        OrcamentoDTO resultado = orcamentoService.salvarOuAtualizar(request, usuario);

        assertEquals(5L, resultado.getId());
        assertEquals(0, BigDecimal.valueOf(450.0).compareTo(resultado.getLimiteMensal()));
        verify(orcamentoRepository).save(existente);
        verify(orcamentoRepository, never()).save(argThat(o -> o != existente));
    }

    @Test
    void deveCalcularPercentualConsumidoCorretamente() {
        Usuario usuario = usuarioComId(1L);
        Orcamento orcamento = orcamentoComLimite(usuario, CategoriaGasto.ALIMENTACAO, 500.0);
        when(orcamentoRepository.findByUsuarioId(1L)).thenReturn(List.of(orcamento));
        when(gastoRepository.findByFiltros(1L, CategoriaGasto.ALIMENTACAO, 7, 2026))
                .thenReturn(List.of(gastoComValor(250.0)));

        List<OrcamentoDTO> resultado = orcamentoService.listarComConsumo(1L, 7, 2026);

        assertEquals(1, resultado.size());
        assertEquals(0, BigDecimal.valueOf(50.0).compareTo(resultado.get(0).getPercentualConsumido()));
        assertEquals("DENTRO_DO_LIMITE", resultado.get(0).getStatus());
    }

    @Test
    void deveMarcarComoEstouradoQuandoConsumoUltrapassaLimite() {
        Usuario usuario = usuarioComId(1L);
        Orcamento orcamento = orcamentoComLimite(usuario, CategoriaGasto.LAZER, 100.0);
        when(orcamentoRepository.findByUsuarioId(1L)).thenReturn(List.of(orcamento));
        when(gastoRepository.findByFiltros(1L, CategoriaGasto.LAZER, 7, 2026))
                .thenReturn(List.of(gastoComValor(150.0)));

        List<OrcamentoDTO> resultado = orcamentoService.listarComConsumo(1L, 7, 2026);

        assertEquals("ESTOURADO", resultado.get(0).getStatus());
    }

    @Test
    void deveMarcarComoAtencaoQuandoConsumoAtinge80Porcento() {
        Usuario usuario = usuarioComId(1L);
        Orcamento orcamento = orcamentoComLimite(usuario, CategoriaGasto.SAUDE, 200.0);
        when(orcamentoRepository.findByUsuarioId(1L)).thenReturn(List.of(orcamento));
        when(gastoRepository.findByFiltros(1L, CategoriaGasto.SAUDE, 7, 2026))
                .thenReturn(List.of(gastoComValor(160.0)));

        List<OrcamentoDTO> resultado = orcamentoService.listarComConsumo(1L, 7, 2026);

        assertEquals("ATENCAO", resultado.get(0).getStatus());
    }

    @Test
    void deveLancarForbiddenAoDeletarOrcamentoDeOutroUsuario() {
        Orcamento orcamento = orcamentoComLimite(usuarioComId(1L), CategoriaGasto.OUTROS, 100.0);
        when(orcamentoRepository.findById(9L)).thenReturn(Optional.of(orcamento));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> orcamentoService.deletar(9L, 2L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(orcamentoRepository, never()).deleteById(any());
    }

    @Test
    void deveLancarNotFoundAoDeletarOrcamentoInexistente() {
        when(orcamentoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> orcamentoService.deletar(99L, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(orcamentoRepository, never()).deleteById(any());
    }

    private Usuario usuarioComId(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }

    private Orcamento orcamentoComLimite(Usuario usuario, CategoriaGasto categoria, double limite) {
        Orcamento orcamento = new Orcamento();
        orcamento.setId(1L);
        orcamento.setCategoria(categoria);
        orcamento.setLimiteMensal(BigDecimal.valueOf(limite));
        orcamento.setUsuario(usuario);
        return orcamento;
    }

    private Gasto gastoComValor(double valor) {
        Gasto gasto = new Gasto();
        gasto.setValor(BigDecimal.valueOf(valor));
        gasto.setData(LocalDate.now());
        return gasto;
    }
}
