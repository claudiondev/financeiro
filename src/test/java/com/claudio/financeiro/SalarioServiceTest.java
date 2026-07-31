package com.claudio.financeiro;

import com.claudio.financeiro.dto.CriarSalarioRequest;
import com.claudio.financeiro.dto.SalarioDTO;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.SalarioRepository;
import com.claudio.financeiro.service.SalarioService;
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
class SalarioServiceTest {

    @Mock
    private SalarioRepository salarioRepository;

    @InjectMocks
    private SalarioService salarioService;

    @Test
    void deveCriarSalarioAPartirDoRequest() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        CriarSalarioRequest request = new CriarSalarioRequest(
                BigDecimal.valueOf(3000.0), BigDecimal.valueOf(200.0), null, "Salário fixo", LocalDate.of(2026, 7, 5)
        );
        when(salarioRepository.save(any(Salario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        SalarioDTO resultado = salarioService.criar(request, usuario);

        assertEquals("Salário fixo", resultado.getDescricao());
        assertEquals(1L, resultado.getUsuarioId());
        assertValorIgual(3000.0, resultado.getValor());
        assertValorIgual(200.0, resultado.getComissao());
    }

    @Test
    void deveAtualizarSalarioQuandoUsuarioEhDono() {
        Salario salarioExistente = salarioComUsuario(1L, 3000.0);
        when(salarioRepository.findById(5L)).thenReturn(Optional.of(salarioExistente));
        when(salarioRepository.save(any(Salario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CriarSalarioRequest request = new CriarSalarioRequest(
                BigDecimal.valueOf(3500.0), BigDecimal.valueOf(300.0), null, "Reajuste", LocalDate.of(2026, 7, 20)
        );

        SalarioDTO resultado = salarioService.atualizar(5L, request, 1L);

        assertEquals("Reajuste", resultado.getDescricao());
        assertValorIgual(3500.0, resultado.getValor());
        assertValorIgual(300.0, resultado.getComissao());
        verify(salarioRepository).save(salarioExistente);
    }

    @Test
    void deveLancarForbiddenAoAtualizarSalarioDeOutroUsuario() {
        Salario salario = salarioComUsuario(1L, 3000.0);
        when(salarioRepository.findById(5L)).thenReturn(Optional.of(salario));
        CriarSalarioRequest request = new CriarSalarioRequest(BigDecimal.TEN, null, null, null, LocalDate.now());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> salarioService.atualizar(5L, request, 2L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(salarioRepository, never()).save(any());
    }

    @Test
    void deveLancarNotFoundAoAtualizarSalarioInexistente() {
        when(salarioRepository.findById(99L)).thenReturn(Optional.empty());
        CriarSalarioRequest request = new CriarSalarioRequest(BigDecimal.TEN, null, null, null, LocalDate.now());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> salarioService.atualizar(99L, request, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(salarioRepository, never()).save(any());
    }

    @Test
    void deveListarSalariosFiltradosPeloUsuario() {
        Salario s1 = salarioComUsuario(1L, 3000.0);
        Salario s2 = salarioComUsuario(1L, 500.0);
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(s1, s2));

        List<SalarioDTO> resultado = salarioService.listarPorUsuario(1L);

        assertEquals(2, resultado.size());
        assertValorIgual(3000.0, resultado.get(0).getValor());
        verify(salarioRepository).findByUsuarioId(1L);
    }

    @Test
    void deveDeletarSalarioQuandoUsuarioEhODono() {
        Salario salario = salarioComUsuario(1L, 3000.0);
        when(salarioRepository.findById(5L)).thenReturn(Optional.of(salario));

        salarioService.deletar(5L, 1L);

        verify(salarioRepository).deleteById(5L);
    }

    // IDOR: usuário 2 tenta deletar salário do usuário 1
    @Test
    void deveLancarForbiddenAoDeletarSalarioDeOutroUsuario() {
        Salario salario = salarioComUsuario(1L, 3000.0);
        when(salarioRepository.findById(5L)).thenReturn(Optional.of(salario));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> salarioService.deletar(5L, 2L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(salarioRepository, never()).deleteById(any());
    }

    @Test
    void deveLancarNotFoundAoDeletarSalarioInexistente() {
        when(salarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> salarioService.deletar(99L, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(salarioRepository, never()).deleteById(any());
    }

    private Salario salarioComUsuario(Long usuarioId, double valor) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Salario salario = new Salario();
        salario.setValor(BigDecimal.valueOf(valor));
        salario.setData(LocalDate.now());
        salario.setUsuario(usuario);
        return salario;
    }

    // BigDecimal.equals() é sensível à escala ("3000.0" != "3000.00") — compareTo é a forma
    // correta de comparar valor numérico independente de como o BigDecimal foi construído.
    private static void assertValorIgual(double esperado, BigDecimal atual) {
        assertEquals(0, BigDecimal.valueOf(esperado).compareTo(atual),
                () -> "Esperado " + esperado + " mas foi " + atual);
    }
}
