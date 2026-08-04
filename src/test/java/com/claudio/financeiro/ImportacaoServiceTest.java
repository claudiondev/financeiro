package com.claudio.financeiro;

import com.claudio.financeiro.dto.ImportacaoResultadoDTO;
import com.claudio.financeiro.dto.ItemConfirmadoDTO;
import com.claudio.financeiro.dto.TransacaoImportadaDTO;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.model.TipoTransacaoImportada;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import com.claudio.financeiro.service.ImportacaoService;
import com.claudio.financeiro.service.OfxParser;
import com.claudio.financeiro.service.TransacaoOfx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportacaoServiceTest {

    @Mock
    private OfxParser ofxParser;

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private SalarioRepository salarioRepository;

    @InjectMocks
    private ImportacaoService importacaoService;

    @Test
    void deveClassificarValorNegativoComoGastoEPositivoComoSalario() {
        List<TransacaoOfx> transacoes = List.of(
                new TransacaoOfx("f1", LocalDate.of(2026, 6, 10), new BigDecimal("-50.00"), "Mercado"),
                new TransacaoOfx("f2", LocalDate.of(2026, 6, 1), new BigDecimal("3000.00"), "Salario")
        );
        when(ofxParser.parse(any())).thenReturn(transacoes);
        when(gastoRepository.findFitidsExistentes(eq(1L), any())).thenReturn(List.of());
        when(salarioRepository.findFitidsExistentes(eq(1L), any())).thenReturn(List.of());

        List<TransacaoImportadaDTO> resultado = importacaoService.processarArquivo(arquivoQualquer(), 1L);

        assertEquals(TipoTransacaoImportada.GASTO, resultado.get(0).getTipo());
        assertEquals(CategoriaGasto.OUTROS, resultado.get(0).getCategoria());
        assertEquals(0, new BigDecimal("50.00").compareTo(resultado.get(0).getValor()));

        assertEquals(TipoTransacaoImportada.SALARIO, resultado.get(1).getTipo());
        assertNull(resultado.get(1).getCategoria());
    }

    @Test
    void deveMarcarComoJaImportadoQuandoFitidJaExiste() {
        List<TransacaoOfx> transacoes = List.of(
                new TransacaoOfx("repetido", LocalDate.of(2026, 6, 10), new BigDecimal("-50.00"), "Mercado")
        );
        when(ofxParser.parse(any())).thenReturn(transacoes);
        when(gastoRepository.findFitidsExistentes(eq(1L), any())).thenReturn(List.of("repetido"));
        when(salarioRepository.findFitidsExistentes(eq(1L), any())).thenReturn(List.of());

        List<TransacaoImportadaDTO> resultado = importacaoService.processarArquivo(arquivoQualquer(), 1L);

        assertTrue(resultado.get(0).isJaImportado());
    }

    @Test
    void deveCriarGastoESalarioAPartirDosItensConfirmados() {
        Usuario usuario = usuarioComId(1L);
        ItemConfirmadoDTO gasto = new ItemConfirmadoDTO("f1", LocalDate.now(), BigDecimal.TEN, "Mercado", TipoTransacaoImportada.GASTO, CategoriaGasto.ALIMENTACAO);
        ItemConfirmadoDTO salario = new ItemConfirmadoDTO("f2", LocalDate.now(), BigDecimal.valueOf(3000), "Salario", TipoTransacaoImportada.SALARIO, null);

        ImportacaoResultadoDTO resultado = importacaoService.confirmar(List.of(gasto, salario), usuario);

        assertEquals(1, resultado.getGastosCriados());
        assertEquals(1, resultado.getSalariosCriados());
        verify(gastoRepository).save(any(Gasto.class));
        verify(salarioRepository).save(any(Salario.class));
    }

    @Test
    void deveRejeitarGastoSemCategoria() {
        Usuario usuario = usuarioComId(1L);
        ItemConfirmadoDTO semCategoria = new ItemConfirmadoDTO("f1", LocalDate.now(), BigDecimal.TEN, "Mercado", TipoTransacaoImportada.GASTO, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> importacaoService.confirmar(List.of(semCategoria), usuario));

        assertEquals(400, ex.getStatusCode().value());
        verify(gastoRepository, never()).save(any());
    }

    @Test
    void naoDeveQuebrarQuandoItemJaFoiImportadoEntreARevisaoEAConfirmacao() {
        Usuario usuario = usuarioComId(1L);
        ItemConfirmadoDTO item = new ItemConfirmadoDTO("f1", LocalDate.now(), BigDecimal.TEN, "Mercado", TipoTransacaoImportada.GASTO, CategoriaGasto.OUTROS);
        when(gastoRepository.save(any(Gasto.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk_gastos_usuario_fitid"));

        ImportacaoResultadoDTO resultado = assertDoesNotThrow(() -> importacaoService.confirmar(List.of(item), usuario));

        assertEquals(0, resultado.getGastosCriados());
    }

    private MockMultipartFile arquivoQualquer() {
        return new MockMultipartFile("arquivo", "extrato.ofx", "text/plain", "conteudo".getBytes());
    }

    private Usuario usuarioComId(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail("claudio@teste.com");
        return usuario;
    }
}
