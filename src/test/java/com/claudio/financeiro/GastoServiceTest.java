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

/**
 * Testes unitários do GastoService.
 *
 * Usamos @ExtendWith(MockitoExtension.class) em vez de @SpringBootTest porque
 * queremos testar APENAS a lógica do service, sem subir o contexto do Spring,
 * sem banco de dados, sem rede. É mais rápido e mais focado.
 *
 * @Mock cria objetos falsos dos repositórios.
 * @InjectMocks cria o GastoService real e injeta os mocks dentro dele.
 */
@ExtendWith(MockitoExtension.class)
class GastoServiceTest {

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private SalarioRepository salarioRepository;

    @InjectMocks
    private GastoService gastoService;

    // -------------------------------------------------------------------------
    // salvar()
    // -------------------------------------------------------------------------

    /**
     * Verifica que salvar() delega ao repositório e retorna o gasto salvo.
     * Confirma que o service não altera o objeto nem ignora o retorno do banco.
     */
    @Test
    void deveSalvarGastoERetornarORegistro() {
        Gasto gasto = gastoComUsuario(1L);
        when(gastoRepository.save(gasto)).thenReturn(gasto);

        Gasto resultado = gastoService.salvar(gasto);

        assertEquals(gasto, resultado);
        verify(gastoRepository).save(gasto);
    }

    // -------------------------------------------------------------------------
    // listarPorUsuario()
    // -------------------------------------------------------------------------

    /**
     * Verifica que listarPorUsuario() filtra pelo ID correto e converte
     * os Gastos para GastoDTOs (sem expor o objeto Usuario inteiro).
     */
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

    // -------------------------------------------------------------------------
    // deletar()
    // -------------------------------------------------------------------------

    /**
     * Cenário feliz: dono do gasto consegue deletar normalmente.
     * Verifica que deleteById é chamado com o ID correto.
     */
    @Test
    void deveDeletarGastoQuandoUsuarioEhODono() {
        Gasto gasto = gastoComUsuario(1L);
        when(gastoRepository.findById(10L)).thenReturn(Optional.of(gasto));

        gastoService.deletar(10L, 1L);

        verify(gastoRepository).deleteById(10L);
    }

    /**
     * Cenário de segurança (IDOR): usuário tenta deletar gasto de outra pessoa.
     * Esperamos 403 FORBIDDEN e garantimos que deleteById NUNCA é chamado.
     *
     * Este é o teste da correção C4 do relatório de segurança.
     */
    @Test
    void deveLancarForbiddenAoDeletarGastoDeOutroUsuario() {
        Gasto gasto = gastoComUsuario(1L); // gasto pertence ao usuário 1
        when(gastoRepository.findById(10L)).thenReturn(Optional.of(gasto));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gastoService.deletar(10L, 2L) // usuário 2 tenta apagar
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(gastoRepository, never()).deleteById(any()); // deleção não ocorre
    }

    /**
     * Cenário de gasto inexistente: retorna 404 NOT FOUND.
     * Garante que o service não tenta deletar algo que não existe.
     */
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

    // -------------------------------------------------------------------------
    // calcularResumo()
    // -------------------------------------------------------------------------

    /**
     * Saldo positivo: salário maior que os gastos.
     * Verifica o cálculo e a mensagem de parabéns.
     */
    @Test
    void deveCalcularResumoComSaldoPositivo() {
        when(gastoRepository.findByUsuarioId(1L)).thenReturn(List.of(gastoSimples(600.0)));
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioSimples(1000.0)));

        ResumoMensal resultado = gastoService.calcularResumo(1L);

        assertEquals(400.0, resultado.getSaldo());
        assertEquals("Parabéns! Você economizou esse mês!", resultado.getMensagem());
    }

    /**
     * Saldo negativo: gastos maiores que o salário.
     * Verifica o cálculo e a mensagem de alerta.
     */
    @Test
    void deveCalcularResumoComSaldoNegativo() {
        when(gastoRepository.findByUsuarioId(1L)).thenReturn(List.of(gastoSimples(1000.0)));
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioSimples(500.0)));

        ResumoMensal resultado = gastoService.calcularResumo(1L);

        assertEquals(-500.0, resultado.getSaldo());
        assertEquals("Atenção! Seus gastos ultrapassaram o salário!", resultado.getMensagem());
    }

    /**
     * Sem gastos no mês: saldo deve ser igual ao salário.
     * Garante que a lista vazia não causa NullPointerException.
     */
    @Test
    void deveCalcularResumoSemGastosNoMes() {
        when(gastoRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(salarioRepository.findByUsuarioId(1L)).thenReturn(List.of(salarioSimples(2000.0)));

        ResumoMensal resultado = gastoService.calcularResumo(1L);

        assertEquals(2000.0, resultado.getSaldo());
        assertEquals(0.0, resultado.getTotalGasto());
    }

    // -------------------------------------------------------------------------
    // Métodos auxiliares — evitam repetição nos testes
    // -------------------------------------------------------------------------

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
