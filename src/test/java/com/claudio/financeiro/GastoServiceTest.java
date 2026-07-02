package com.claudio.financeiro;

import com.claudio.financeiro.dto.GastoDTO;
import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import com.claudio.financeiro.service.GastoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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
    void deveListarGastosFiltradosPeloUsuario() {
        Gasto g1 = gastoComValor(1L, "Aluguel", 1500.0);
        Gasto g2 = gastoComValor(1L, "Mercado", 300.0);
        when(gastoRepository.findByUsuarioId(1L)).thenReturn(List.of(g1, g2));

        List<GastoDTO> resultado = gastoService.listarPorUsuario(1L);

        assertEquals(2, resultado.size());
        assertEquals("Aluguel", resultado.get(0).getDescricao());
        assertEquals(300.0, resultado.get(1).getValor());
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

        assertEquals(400.0, resultado.getSaldo());
        assertEquals("Parabéns! Você economizou esse mês!", resultado.getMensagem());
    }

    @Test
    void deveCalcularResumoComSaldoNegativo() {
        when(gastoRepository.findByUsuarioId(1L)).thenReturn(List.of(gastoSimples(1000.0)));
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioSimples(500.0)));

        ResumoMensal resultado = gastoService.calcularResumo(1L);

        assertEquals(-500.0, resultado.getSaldo());
        assertEquals("Atenção! Seus gastos ultrapassaram o salário!", resultado.getMensagem());
    }

    @Test
    void deveCalcularResumoSemGastosNoMes() {
        when(gastoRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioSimples(2000.0)));

        ResumoMensal resultado = gastoService.calcularResumo(1L);

        assertEquals(2000.0, resultado.getSaldo());
        assertEquals(0.0, resultado.getTotalGasto());
    }

    private Gasto gastoComUsuario(Long usuarioId) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Gasto gasto = new Gasto();
        gasto.setDescricao("Teste");
        gasto.setValor(100.0);
        gasto.setCategoria("Outros");
        gasto.setData(LocalDate.now());
        gasto.setUsuario(usuario);
        return gasto;
    }

    private Gasto gastoComValor(Long usuarioId, String descricao, Double valor) {
        Gasto gasto = gastoComUsuario(usuarioId);
        gasto.setDescricao(descricao);
        gasto.setValor(valor);
        return gasto;
    }

    private Gasto gastoSimples(Double valor) {
        Gasto gasto = new Gasto();
        gasto.setValor(valor);
        return gasto;
    }

    private Salario salarioSimples(Double valor) {
        Salario salario = new Salario();
        salario.setValor(valor);
        salario.setComissao(0.0);
        salario.setAdicional(0.0);
        return salario;
    }
}
