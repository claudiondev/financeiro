package com.claudio.financeiro;

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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do SalarioService.
 *
 * Cobre os mesmos cenários críticos de segurança do GastoService:
 * deleção com verificação de ownership (proteção contra IDOR) e
 * comportamento correto quando o registro não existe (404).
 */
@ExtendWith(MockitoExtension.class)
class SalarioServiceTest {

    @Mock
    private SalarioRepository salarioRepository;

    @InjectMocks
    private SalarioService salarioService;

    // -------------------------------------------------------------------------
    // salvar()
    // -------------------------------------------------------------------------

    /**
     * Verifica que salvar() persiste o salário e retorna o objeto salvo.
     */
    @Test
    void deveSalvarSalarioERetornarORegistro() {
        Salario salario = salarioComUsuario(1L, 3000.0);
        when(salarioRepository.save(salario)).thenReturn(salario);

        Salario resultado = salarioService.salvar(salario);

        assertEquals(salario, resultado);
        verify(salarioRepository).save(salario);
    }

    // -------------------------------------------------------------------------
    // listarPorUsuario()
    // -------------------------------------------------------------------------

    /**
     * Verifica que listarPorUsuario() usa o ID correto como filtro.
     * Importante: não deve retornar salários de outros usuários.
     */
    @Test
    void deveListarSalariosFiltradosPeloUsuario() {
        Salario s1 = salarioComUsuario(1L, 3000.0);
        Salario s2 = salarioComUsuario(1L, 500.0);
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(s1, s2));

        List<Salario> resultado = salarioService.listarPorUsuario(1L);

        assertEquals(2, resultado.size());
        assertEquals(3000.0, resultado.get(0).getValor());
        verify(salarioRepository).findByUsuarioId(1L);
    }

    // -------------------------------------------------------------------------
    // deletar()
    // -------------------------------------------------------------------------

    /**
     * Cenário feliz: dono do salário consegue deletar normalmente.
     */
    @Test
    void deveDeletarSalarioQuandoUsuarioEhODono() {
        Salario salario = salarioComUsuario(1L, 3000.0);
        when(salarioRepository.findById(5L)).thenReturn(Optional.of(salario));

        salarioService.deletar(5L, 1L);

        verify(salarioRepository).deleteById(5L);
    }

    /**
     * Cenário de segurança (IDOR): usuário tenta deletar salário de outra pessoa.
     * Esperamos 403 FORBIDDEN e garantimos que deleteById NUNCA é chamado.
     *
     * Este é o teste da correção C4 do relatório de segurança (SalarioService).
     */
    @Test
    void deveLancarForbiddenAoDeletarSalarioDeOutroUsuario() {
        Salario salario = salarioComUsuario(1L, 3000.0); // pertence ao usuário 1
        when(salarioRepository.findById(5L)).thenReturn(Optional.of(salario));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> salarioService.deletar(5L, 2L) // usuário 2 tenta apagar
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(salarioRepository, never()).deleteById(any());
    }

    /**
     * Cenário de salário inexistente: retorna 404 NOT FOUND.
     */
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

    // -------------------------------------------------------------------------
    // Método auxiliar
    // -------------------------------------------------------------------------

    private Salario salarioComUsuario(Long usuarioId, Double valor) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Salario salario = new Salario();
        salario.setValor(valor);
        salario.setData(LocalDate.now());
        salario.setUsuario(usuario);
        return salario;
    }
}
