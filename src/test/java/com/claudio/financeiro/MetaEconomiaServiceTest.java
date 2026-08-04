package com.claudio.financeiro;

import com.claudio.financeiro.dto.CriarMetaEconomiaRequest;
import com.claudio.financeiro.dto.MetaEconomiaDTO;
import com.claudio.financeiro.dto.RegistrarAporteRequest;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.MetaEconomia;
import com.claudio.financeiro.model.StatusMetaEconomia;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.MetaEconomiaRepository;
import com.claudio.financeiro.service.MetaEconomiaService;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetaEconomiaServiceTest {

    @Mock
    private MetaEconomiaRepository metaEconomiaRepository;

    @Mock
    private GastoRepository gastoRepository;

    @InjectMocks
    private MetaEconomiaService metaEconomiaService;

    @Test
    void deveCriarMetaComOsDadosDoRequest() {
        Usuario usuario = usuarioComId(1L);
        when(metaEconomiaRepository.save(any(MetaEconomia.class))).thenAnswer(inv -> {
            MetaEconomia m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });
        when(gastoRepository.somarValorPorMetaEconomia(10L)).thenReturn(BigDecimal.ZERO);

        CriarMetaEconomiaRequest request = new CriarMetaEconomiaRequest("Viagem", BigDecimal.valueOf(5000), LocalDate.of(2026, 12, 1));
        MetaEconomiaDTO resultado = metaEconomiaService.criar(request, usuario);

        assertEquals("Viagem", resultado.getNome());
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(resultado.getValorAlvo()));
        assertEquals(StatusMetaEconomia.EM_ANDAMENTO, resultado.getStatus());
    }

    @Test
    void deveCalcularPercentualEStatusEmAndamento() {
        MetaEconomia meta = metaComAlvo(1L, usuarioComId(1L), 1000.0, null);
        when(metaEconomiaRepository.findByUsuarioId(1L)).thenReturn(List.of(meta));
        when(gastoRepository.somarValorPorMetaEconomia(1L)).thenReturn(BigDecimal.valueOf(400.0));

        List<MetaEconomiaDTO> resultado = metaEconomiaService.listarComProgresso(1L);

        assertEquals(0, BigDecimal.valueOf(40.0).compareTo(resultado.get(0).getPercentualConcluido()));
        assertEquals(StatusMetaEconomia.EM_ANDAMENTO, resultado.get(0).getStatus());
    }

    @Test
    void deveMarcarComoConcluidaQuandoAcumuladoAtingeOAlvo() {
        MetaEconomia meta = metaComAlvo(1L, usuarioComId(1L), 1000.0, null);
        when(metaEconomiaRepository.findByUsuarioId(1L)).thenReturn(List.of(meta));
        when(gastoRepository.somarValorPorMetaEconomia(1L)).thenReturn(BigDecimal.valueOf(1000.0));

        List<MetaEconomiaDTO> resultado = metaEconomiaService.listarComProgresso(1L);

        assertEquals(StatusMetaEconomia.CONCLUIDA, resultado.get(0).getStatus());
    }

    @Test
    void deveMarcarComoAtrasadaQuandoPrazoPassouSemAtingirOAlvo() {
        MetaEconomia meta = metaComAlvo(1L, usuarioComId(1L), 1000.0, LocalDate.now().minusDays(1));
        when(metaEconomiaRepository.findByUsuarioId(1L)).thenReturn(List.of(meta));
        when(gastoRepository.somarValorPorMetaEconomia(1L)).thenReturn(BigDecimal.valueOf(400.0));

        List<MetaEconomiaDTO> resultado = metaEconomiaService.listarComProgresso(1L);

        assertEquals(StatusMetaEconomia.ATRASADA, resultado.get(0).getStatus());
    }

    @Test
    void naoDeveMarcarComoAtrasadaSeJaConcluidaMesmoComPrazoVencido() {
        MetaEconomia meta = metaComAlvo(1L, usuarioComId(1L), 1000.0, LocalDate.now().minusDays(1));
        when(metaEconomiaRepository.findByUsuarioId(1L)).thenReturn(List.of(meta));
        when(gastoRepository.somarValorPorMetaEconomia(1L)).thenReturn(BigDecimal.valueOf(1500.0));

        List<MetaEconomiaDTO> resultado = metaEconomiaService.listarComProgresso(1L);

        assertEquals(StatusMetaEconomia.CONCLUIDA, resultado.get(0).getStatus());
    }

    @Test
    void registrarAporteDeveCriarGastoDeCategoriaPoupancaVinculadoAMeta() {
        Usuario usuario = usuarioComId(1L);
        MetaEconomia meta = metaComAlvo(5L, usuario, 1000.0, null);
        when(metaEconomiaRepository.findById(5L)).thenReturn(Optional.of(meta));
        when(gastoRepository.somarValorPorMetaEconomia(5L)).thenReturn(BigDecimal.valueOf(200.0));

        RegistrarAporteRequest request = new RegistrarAporteRequest(BigDecimal.valueOf(200.0), LocalDate.now());
        MetaEconomiaDTO resultado = metaEconomiaService.registrarAporte(5L, request, usuario);

        ArgumentCaptor<Gasto> captor = ArgumentCaptor.forClass(Gasto.class);
        verify(gastoRepository).save(captor.capture());
        Gasto salvo = captor.getValue();
        assertEquals(CategoriaGasto.POUPANCA, salvo.getCategoria());
        assertEquals(meta, salvo.getMetaEconomia());
        assertEquals(0, BigDecimal.valueOf(200.0).compareTo(salvo.getValor()));
        assertEquals(0, BigDecimal.valueOf(200.0).compareTo(resultado.getValorAcumulado()));
    }

    @Test
    void deveLancarForbiddenAoRegistrarAporteEmMetaDeOutroUsuario() {
        MetaEconomia meta = metaComAlvo(5L, usuarioComId(1L), 1000.0, null);
        when(metaEconomiaRepository.findById(5L)).thenReturn(Optional.of(meta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> metaEconomiaService.registrarAporte(5L, new RegistrarAporteRequest(BigDecimal.TEN, LocalDate.now()), usuarioComId(2L)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(gastoRepository, never()).save(any());
    }

    @Test
    void deveLancarNotFoundAoDeletarMetaInexistente() {
        when(metaEconomiaRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> metaEconomiaService.deletar(99L, 1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(metaEconomiaRepository, never()).deleteById(any());
    }

    private Usuario usuarioComId(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }

    private MetaEconomia metaComAlvo(Long id, Usuario usuario, double alvo, LocalDate prazo) {
        MetaEconomia meta = new MetaEconomia();
        meta.setId(id);
        meta.setNome("Meta teste");
        meta.setValorAlvo(BigDecimal.valueOf(alvo));
        meta.setPrazo(prazo);
        meta.setUsuario(usuario);
        return meta;
    }
}
