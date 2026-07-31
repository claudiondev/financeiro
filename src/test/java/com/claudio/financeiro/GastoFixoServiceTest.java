package com.claudio.financeiro;

import com.claudio.financeiro.dto.CriarGastoFixoRequest;
import com.claudio.financeiro.dto.GastoFixoDTO;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.GastoFixo;
import com.claudio.financeiro.model.StatusGastoFixo;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoFixoRepository;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.service.GastoFixoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GastoFixoServiceTest {

    @Mock
    private GastoFixoRepository gastoFixoRepository;

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private GastoFixoService gastoFixoService;

    @Test
    void deveGerarGastoQuandoAindaNaoExisteNoMes() {
        GastoFixo fixo = fixoComId(10L, usuarioComId(1L), 5);
        YearMonth mesAtual = YearMonth.now();

        when(gastoFixoRepository.findByUsuarioIdAndAtivoTrue(1L)).thenReturn(List.of(fixo));
        when(gastoRepository.existsByGastoFixoIdAndDataBetween(10L, mesAtual.atDay(1), mesAtual.atEndOfMonth()))
                .thenReturn(false);
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> inv.getArgument(0));

        gastoFixoService.garantirGastosDoMesGerados(1L, mesAtual.getMonthValue(), mesAtual.getYear());

        ArgumentCaptor<Gasto> captor = ArgumentCaptor.forClass(Gasto.class);
        verify(gastoRepository).save(captor.capture());
        Gasto salvo = captor.getValue();
        assertFalse(salvo.isPago());
        assertEquals(fixo, salvo.getGastoFixo());
        assertEquals(CategoriaGasto.MORADIA, salvo.getCategoria());
        assertValorIgual(500.0, salvo.getValor());
    }

    @Test
    void naoDeveDuplicarGastoSeJaFoiGeradoNoMes() {
        GastoFixo fixo = fixoComId(10L, usuarioComId(1L), 5);
        YearMonth mesAtual = YearMonth.now();

        when(gastoFixoRepository.findByUsuarioIdAndAtivoTrue(1L)).thenReturn(List.of(fixo));
        when(gastoRepository.existsByGastoFixoIdAndDataBetween(10L, mesAtual.atDay(1), mesAtual.atEndOfMonth()))
                .thenReturn(true);

        gastoFixoService.garantirGastosDoMesGerados(1L, mesAtual.getMonthValue(), mesAtual.getYear());

        verify(gastoRepository, never()).save(any());
    }

    @Test
    void naoDeveGerarQuandoDataInicioForNoFuturo() {
        GastoFixo fixo = fixoComId(10L, usuarioComId(1L), 5);
        fixo.setDataInicio(LocalDate.now().plusMonths(2));

        when(gastoFixoRepository.findByUsuarioIdAndAtivoTrue(1L)).thenReturn(List.of(fixo));

        gastoFixoService.garantirGastosDoMesGerados(1L, LocalDate.now().getMonthValue(), LocalDate.now().getYear());

        verify(gastoRepository, never()).existsByGastoFixoIdAndDataBetween(any(), any(), any());
        verify(gastoRepository, never()).save(any());
    }

    @Test
    void deveAjustarDiaDeVencimentoQuandoMesForMaisCurto() {
        GastoFixo fixo = fixoComId(10L, usuarioComId(1L), 31);
        fixo.setDataInicio(LocalDate.of(2025, 1, 1));
        YearMonth fevereiro2026 = YearMonth.of(2026, 2); // 2026 não é bissexto, fevereiro tem 28 dias

        when(gastoFixoRepository.findByUsuarioIdAndAtivoTrue(1L)).thenReturn(List.of(fixo));
        when(gastoRepository.existsByGastoFixoIdAndDataBetween(10L, fevereiro2026.atDay(1), fevereiro2026.atEndOfMonth()))
                .thenReturn(false);
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> inv.getArgument(0));

        gastoFixoService.garantirGastosDoMesGerados(1L, 2, 2026);

        ArgumentCaptor<Gasto> captor = ArgumentCaptor.forClass(Gasto.class);
        verify(gastoRepository).save(captor.capture());
        assertEquals(LocalDate.of(2026, 2, 28), captor.getValue().getData());
    }

    @Test
    void deveCriarGastoFixoAPartirDoRequest() {
        Usuario usuario = usuarioComId(1L);
        CriarGastoFixoRequest request = new CriarGastoFixoRequest(
                CategoriaGasto.MORADIA, BigDecimal.valueOf(1200.0), "Aluguel", 10, LocalDate.now().minusMonths(1)
        );
        when(gastoFixoRepository.save(any(GastoFixo.class))).thenAnswer(inv -> {
            GastoFixo salvo = inv.getArgument(0);
            salvo.setId(20L);
            return salvo;
        });
        when(gastoRepository.findByGastoFixoIdAndDataBetween(eq(20L), any(), any())).thenReturn(Optional.empty());

        GastoFixoDTO resultado = gastoFixoService.criar(request, usuario);

        assertEquals("Aluguel", resultado.getDescricao());
        assertEquals(10, resultado.getDiaVencimento());
        assertTrue(resultado.isAtivo());
    }

    @Test
    void deveAtualizarGastoFixoQuandoUsuarioEhDono() {
        GastoFixo existente = fixoComId(10L, usuarioComId(1L), 5);
        when(gastoFixoRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(gastoFixoRepository.save(any(GastoFixo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gastoRepository.findByGastoFixoIdAndDataBetween(eq(10L), any(), any())).thenReturn(Optional.empty());

        CriarGastoFixoRequest request = new CriarGastoFixoRequest(
                CategoriaGasto.LAZER, BigDecimal.valueOf(60.0), "Streaming", 15, LocalDate.now().minusMonths(2)
        );
        GastoFixoDTO resultado = gastoFixoService.atualizar(10L, request, 1L);

        assertEquals("Streaming", resultado.getDescricao());
        assertEquals(CategoriaGasto.LAZER, resultado.getCategoria());
        assertEquals(15, resultado.getDiaVencimento());
    }

    @Test
    void deveLancarForbiddenAoAtualizarGastoFixoDeOutroUsuario() {
        GastoFixo fixo = fixoComId(10L, usuarioComId(1L), 5);
        when(gastoFixoRepository.findById(10L)).thenReturn(Optional.of(fixo));
        CriarGastoFixoRequest request = new CriarGastoFixoRequest(CategoriaGasto.OUTROS, BigDecimal.TEN, "X", 1, LocalDate.now());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gastoFixoService.atualizar(10L, request, 2L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(gastoFixoRepository, never()).save(any());
    }

    @Test
    void deveDeletarGastoFixoQuandoUsuarioEhDono() {
        GastoFixo fixo = fixoComId(10L, usuarioComId(1L), 5);
        when(gastoFixoRepository.findById(10L)).thenReturn(Optional.of(fixo));

        gastoFixoService.deletar(10L, 1L);

        verify(gastoFixoRepository).deleteById(10L);
    }

    // IDOR: usuário 2 tenta deletar gasto fixo do usuário 1
    @Test
    void deveLancarForbiddenAoDeletarGastoFixoDeOutroUsuario() {
        GastoFixo fixo = fixoComId(10L, usuarioComId(1L), 5);
        when(gastoFixoRepository.findById(10L)).thenReturn(Optional.of(fixo));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gastoFixoService.deletar(10L, 2L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(gastoFixoRepository, never()).deleteById(any());
    }

    @Test
    void devePausarGastoFixoSemApagarOMolde() {
        GastoFixo fixo = fixoComId(10L, usuarioComId(1L), 5);
        when(gastoFixoRepository.findById(10L)).thenReturn(Optional.of(fixo));
        when(gastoFixoRepository.save(any(GastoFixo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gastoRepository.findByGastoFixoIdAndDataBetween(eq(10L), any(), any())).thenReturn(Optional.empty());

        GastoFixoDTO resultado = gastoFixoService.pausar(10L, 1L);

        assertFalse(resultado.isAtivo());
        verify(gastoFixoRepository, never()).deleteById(any());
    }

    @Test
    void deveReativarGastoFixoPausado() {
        GastoFixo fixo = fixoComId(10L, usuarioComId(1L), 5);
        fixo.setAtivo(false);
        when(gastoFixoRepository.findById(10L)).thenReturn(Optional.of(fixo));
        when(gastoFixoRepository.save(any(GastoFixo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gastoRepository.findByGastoFixoIdAndDataBetween(eq(10L), any(), any())).thenReturn(Optional.empty());

        GastoFixoDTO resultado = gastoFixoService.reativar(10L, 1L);

        assertTrue(resultado.isAtivo());
    }

    @Test
    void deveRetornarStatusAtrasadoQuandoVencimentoJaPassouENaoPago() {
        List<GastoFixoDTO> resultado = listarComStatusMockandoGastoDoMes(false, LocalDate.now().minusDays(2));
        assertEquals(StatusGastoFixo.ATRASADO, resultado.get(0).getStatusMesAtual());
    }

    @Test
    void deveRetornarStatusVencendoQuandoFaltamAteTresDias() {
        List<GastoFixoDTO> resultado = listarComStatusMockandoGastoDoMes(false, LocalDate.now().plusDays(2));
        assertEquals(StatusGastoFixo.VENCENDO, resultado.get(0).getStatusMesAtual());
    }

    @Test
    void deveRetornarStatusPendenteQuandoFaltamMaisDeTresDias() {
        List<GastoFixoDTO> resultado = listarComStatusMockandoGastoDoMes(false, LocalDate.now().plusDays(10));
        assertEquals(StatusGastoFixo.PENDENTE, resultado.get(0).getStatusMesAtual());
    }

    @Test
    void deveRetornarStatusPagoQuandoGastoDoMesJaFoiPago() {
        List<GastoFixoDTO> resultado = listarComStatusMockandoGastoDoMes(true, LocalDate.now().minusDays(2));
        assertEquals(StatusGastoFixo.PAGO, resultado.get(0).getStatusMesAtual());
    }

    @Test
    void naoDeveEnviarEmailQuandoFlagEstaDesabilitada() {
        // Padrão hoje: desligado até o .env ter credenciais de e-mail reais (ver application.properties).
        // A flag desligada retorna antes até de buscar o Gasto por id — não precisa desse mock aqui.
        Usuario usuario = usuarioComId(1L);
        listarComStatusMockandoGastoDoMes(false, LocalDate.now().plusDays(1));

        gastoFixoService.listarPendentesAlerta(usuario);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void deveEnviarEmailDeLembretePraContaVencendoQuandoHabilitado() {
        ReflectionTestUtils.setField(gastoFixoService, "lembretePorEmailHabilitado", true);
        Usuario usuario = usuarioComId(1L);
        Gasto gastoVencendo = configurarMockParaAlerta(usuario, false, LocalDate.now().plusDays(1), null);

        gastoFixoService.listarPendentesAlerta(usuario);

        verify(mailSender).send(any(SimpleMailMessage.class));
        assertEquals(LocalDate.now(), gastoVencendo.getUltimoLembreteEnviadoEm());
    }

    @Test
    void naoDeveReenviarEmailSeJaMandouHoje() {
        ReflectionTestUtils.setField(gastoFixoService, "lembretePorEmailHabilitado", true);
        Usuario usuario = usuarioComId(1L);
        configurarMockParaAlerta(usuario, false, LocalDate.now().plusDays(1), LocalDate.now());

        gastoFixoService.listarPendentesAlerta(usuario);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // Falha de SMTP (ex: credencial de e-mail não configurada) não pode derrubar o
    // endpoint — o alerta dentro do app tem que continuar funcionando mesmo assim.
    @Test
    void naoDeveQuebrarQuandoEnvioDeEmailFalha() {
        ReflectionTestUtils.setField(gastoFixoService, "lembretePorEmailHabilitado", true);
        Usuario usuario = usuarioComId(1L);
        configurarMockParaAlerta(usuario, false, LocalDate.now().plusDays(1), null);
        doThrow(new org.springframework.mail.MailAuthenticationException("Authentication failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        List<GastoFixoDTO> resultado = gastoFixoService.listarPendentesAlerta(usuario);

        assertEquals(1, resultado.size());
    }

    // Monta o cenário comum aos testes de status: um GastoFixo com um Gasto do mês corrente
    // já gerado, controlando só se está pago e qual a data de vencimento.
    private List<GastoFixoDTO> listarComStatusMockandoGastoDoMes(boolean pago, LocalDate dataVencimento) {
        Usuario usuario = usuarioComId(1L);
        GastoFixo fixo = fixoComId(10L, usuario, 5);
        YearMonth mesAtual = YearMonth.now();

        when(gastoFixoRepository.findByUsuarioIdAndAtivoTrue(1L)).thenReturn(List.of(fixo));
        when(gastoRepository.existsByGastoFixoIdAndDataBetween(10L, mesAtual.atDay(1), mesAtual.atEndOfMonth()))
                .thenReturn(true);
        when(gastoFixoRepository.findByUsuarioId(1L)).thenReturn(List.of(fixo));

        Gasto gastoDoMes = new Gasto();
        gastoDoMes.setId(50L);
        gastoDoMes.setPago(pago);
        gastoDoMes.setData(dataVencimento);
        when(gastoRepository.findByGastoFixoIdAndDataBetween(10L, mesAtual.atDay(1), mesAtual.atEndOfMonth()))
                .thenReturn(Optional.of(gastoDoMes));

        return gastoFixoService.listarComStatus(1L);
    }

    private Gasto configurarMockParaAlerta(Usuario usuario, boolean pago, LocalDate dataVencimento, LocalDate ultimoLembrete) {
        GastoFixo fixo = fixoComId(10L, usuario, 5);
        YearMonth mesAtual = YearMonth.now();

        when(gastoFixoRepository.findByUsuarioIdAndAtivoTrue(1L)).thenReturn(List.of(fixo));
        when(gastoRepository.existsByGastoFixoIdAndDataBetween(10L, mesAtual.atDay(1), mesAtual.atEndOfMonth()))
                .thenReturn(true);
        when(gastoFixoRepository.findByUsuarioId(1L)).thenReturn(List.of(fixo));

        Gasto gastoDoMes = new Gasto();
        gastoDoMes.setId(50L);
        gastoDoMes.setPago(pago);
        gastoDoMes.setData(dataVencimento);
        gastoDoMes.setUltimoLembreteEnviadoEm(ultimoLembrete);
        when(gastoRepository.findByGastoFixoIdAndDataBetween(10L, mesAtual.atDay(1), mesAtual.atEndOfMonth()))
                .thenReturn(Optional.of(gastoDoMes));
        when(gastoRepository.findById(50L)).thenReturn(Optional.of(gastoDoMes));

        return gastoDoMes;
    }

    private Usuario usuarioComId(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail("claudio@teste.com");
        return usuario;
    }

    private GastoFixo fixoComId(Long id, Usuario usuario, int diaVencimento) {
        GastoFixo fixo = new GastoFixo();
        fixo.setId(id);
        fixo.setUsuario(usuario);
        fixo.setCategoria(CategoriaGasto.MORADIA);
        fixo.setValor(BigDecimal.valueOf(500.0));
        fixo.setDescricao("Aluguel");
        fixo.setDiaVencimento(diaVencimento);
        fixo.setDataInicio(LocalDate.now().minusMonths(1));
        fixo.setAtivo(true);
        return fixo;
    }

    private static void assertValorIgual(double esperado, BigDecimal atual) {
        assertEquals(0, BigDecimal.valueOf(esperado).compareTo(atual),
                () -> "Esperado " + esperado + " mas foi " + atual);
    }
}
