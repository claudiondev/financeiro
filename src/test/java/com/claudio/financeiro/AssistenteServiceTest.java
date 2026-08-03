package com.claudio.financeiro;

import com.claudio.financeiro.dto.InsightDTO;
import com.claudio.financeiro.dto.OrcamentoDTO;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Severidade;
import com.claudio.financeiro.model.StatusOrcamento;
import com.claudio.financeiro.model.TipoInsight;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.service.AssistenteService;
import com.claudio.financeiro.service.OrcamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistenteServiceTest {

    @Mock
    private OrcamentoService orcamentoService;

    @Mock
    private GastoRepository gastoRepository;

    @InjectMocks
    private AssistenteService assistenteService;

    private final LocalDate hoje = LocalDate.now();
    private final YearMonth mesAnterior = YearMonth.from(hoje).minusMonths(1);

    @Test
    void deveGerarInsightDeOrcamentoEstourado() {
        semGastosNoMesAtualEAnterior();
        mockarOrcamentos(orcamentoComConsumo(CategoriaGasto.LAZER, 100.0, 150.0, StatusOrcamento.ESTOURADO));

        List<InsightDTO> insights = assistenteService.gerarInsights(1L);

        assertTrue(insights.stream().anyMatch(i -> i.getTipo() == TipoInsight.ORCAMENTO_ESTOURADO
                && i.getSeveridade() == Severidade.CRITICO && i.getCategoria() == CategoriaGasto.LAZER));
    }

    @Test
    void deveGerarInsightDeOrcamentoAtencao() {
        semGastosNoMesAtualEAnterior();
        mockarOrcamentos(orcamentoComConsumo(CategoriaGasto.SAUDE, 200.0, 170.0, StatusOrcamento.ATENCAO));

        List<InsightDTO> insights = assistenteService.gerarInsights(1L);

        assertTrue(insights.stream().anyMatch(i -> i.getTipo() == TipoInsight.ORCAMENTO_ATENCAO
                && i.getSeveridade() == Severidade.ATENCAO && i.getCategoria() == CategoriaGasto.SAUDE));
    }

    @Test
    void naoDeveGerarInsightDeOrcamentoDentroDoLimite() {
        semGastosNoMesAtualEAnterior();
        mockarOrcamentos(orcamentoComConsumo(CategoriaGasto.MORADIA, 1000.0, 300.0, StatusOrcamento.DENTRO_DO_LIMITE));

        List<InsightDTO> insights = assistenteService.gerarInsights(1L);

        assertTrue(insights.stream().noneMatch(i -> i.getTipo() == TipoInsight.ORCAMENTO_ESTOURADO
                || i.getTipo() == TipoInsight.ORCAMENTO_ATENCAO));
    }

    @Test
    void deveGerarInsightDeRitmoQuandoConsumoJaUltrapassaSomaDosOrcamentos() {
        semGastosNoMesAtualEAnterior();
        // Consumido (150) já maior que o limite (100) — a projeção só cresce a partir daqui, então
        // o insight de ritmo dispara em qualquer dia do mês, sem depender de quando o teste rodar.
        mockarOrcamentos(orcamentoComConsumo(CategoriaGasto.LAZER, 100.0, 150.0, StatusOrcamento.ESTOURADO));

        List<InsightDTO> insights = assistenteService.gerarInsights(1L);

        assertTrue(insights.stream().anyMatch(i -> i.getTipo() == TipoInsight.RITMO_ACIMA_DO_ORCAMENTO
                && i.getSeveridade() == Severidade.CRITICO));
    }

    @Test
    void naoDeveGerarInsightDeRitmoSemNenhumOrcamentoCadastrado() {
        semOrcamentos();
        semGastosNoMesAtualEAnterior();

        List<InsightDTO> insights = assistenteService.gerarInsights(1L);

        assertTrue(insights.stream().noneMatch(i -> i.getTipo() == TipoInsight.RITMO_ACIMA_DO_ORCAMENTO));
    }

    @Test
    void deveGerarInsightDeCategoriaEmAltaQuandoCrescimentoSignificativo() {
        semOrcamentos();
        gastosDoMes(hoje, gastoComCategoria(CategoriaGasto.TRANSPORTE, 200.0));
        gastosDoMes(mesAnterior, gastoComCategoria(CategoriaGasto.TRANSPORTE, 100.0));

        List<InsightDTO> insights = assistenteService.gerarInsights(1L);

        assertTrue(insights.stream().anyMatch(i -> i.getTipo() == TipoInsight.CATEGORIA_EM_ALTA
                && i.getCategoria() == CategoriaGasto.TRANSPORTE));
    }

    @Test
    void naoDeveGerarCategoriaEmAltaQuandoAumentoAbsolutoEhPequeno() {
        semOrcamentos();
        // Crescimento de 100% (dobrou), mas só R$10 de aumento — não é relevante o suficiente pra alertar.
        gastosDoMes(hoje, gastoComCategoria(CategoriaGasto.LAZER, 20.0));
        gastosDoMes(mesAnterior, gastoComCategoria(CategoriaGasto.LAZER, 10.0));

        List<InsightDTO> insights = assistenteService.gerarInsights(1L);

        assertTrue(insights.stream().noneMatch(i -> i.getTipo() == TipoInsight.CATEGORIA_EM_ALTA));
    }

    @Test
    void naoDeveGerarCategoriaEmAltaQuandoMesAnteriorNaoTeveGasto() {
        semOrcamentos();
        gastosDoMes(hoje, gastoComCategoria(CategoriaGasto.EDUCACAO, 300.0));
        gastosDoMes(mesAnterior);

        List<InsightDTO> insights = assistenteService.gerarInsights(1L);

        assertTrue(insights.stream().noneMatch(i -> i.getTipo() == TipoInsight.CATEGORIA_EM_ALTA));
    }

    @Test
    void deveSelecionarDicaDaCategoriaComMaiorGastoDoMes() {
        semOrcamentos();
        gastosDoMes(hoje, gastoComCategoria(CategoriaGasto.ALIMENTACAO, 500.0), gastoComCategoria(CategoriaGasto.LAZER, 50.0));
        gastosDoMes(mesAnterior);

        List<InsightDTO> insights = assistenteService.gerarInsights(1L);

        assertTrue(insights.stream().anyMatch(i -> i.getTipo() == TipoInsight.DICA_EDUCACIONAL
                && i.getSeveridade() == Severidade.INFO && i.getCategoria() == CategoriaGasto.ALIMENTACAO));
    }

    @Test
    void deveLimitarATop5InsightsOrdenadosPorSeveridadeDoMaisGraveAoMenosGrave() {
        // Consumido total (150+170+190=510) já ultrapassa a soma dos orçamentos (500) — garante que
        // o insight de ritmo dispare em qualquer dia do mês em que o teste rodar.
        mockarOrcamentos(
                orcamentoComConsumo(CategoriaGasto.LAZER, 100.0, 150.0, StatusOrcamento.ESTOURADO),
                orcamentoComConsumo(CategoriaGasto.SAUDE, 200.0, 170.0, StatusOrcamento.ATENCAO),
                orcamentoComConsumo(CategoriaGasto.EDUCACAO, 200.0, 190.0, StatusOrcamento.ATENCAO)
        );
        gastosDoMes(hoje,
                gastoComCategoria(CategoriaGasto.ALIMENTACAO, 500.0),
                gastoComCategoria(CategoriaGasto.TRANSPORTE, 300.0));
        gastosDoMes(mesAnterior,
                gastoComCategoria(CategoriaGasto.ALIMENTACAO, 100.0),
                gastoComCategoria(CategoriaGasto.TRANSPORTE, 100.0));

        List<InsightDTO> insights = assistenteService.gerarInsights(1L);

        assertEquals(5, insights.size());
        for (int i = 0; i < insights.size() - 1; i++) {
            assertTrue(insights.get(i).getSeveridade().compareTo(insights.get(i + 1).getSeveridade()) <= 0);
        }
    }

    private void semOrcamentos() {
        lenient().when(orcamentoService.listarComConsumo(eq(1L), anyInt(), anyInt())).thenReturn(List.of());
    }

    private void mockarOrcamentos(OrcamentoDTO... orcamentos) {
        when(orcamentoService.listarComConsumo(1L, hoje.getMonthValue(), hoje.getYear())).thenReturn(List.of(orcamentos));
    }

    private void semGastosNoMesAtualEAnterior() {
        gastosDoMes(hoje);
        gastosDoMes(mesAnterior);
    }

    private void gastosDoMes(LocalDate mes, Gasto... gastos) {
        gastosDoMes(YearMonth.from(mes), gastos);
    }

    private void gastosDoMes(YearMonth mes, Gasto... gastos) {
        lenient().when(gastoRepository.findByFiltros(eq(1L), isNull(), eq(mes.getMonthValue()), eq(mes.getYear())))
                .thenReturn(List.of(gastos));
    }

    private OrcamentoDTO orcamentoComConsumo(CategoriaGasto categoria, double limite, double consumido, StatusOrcamento status) {
        BigDecimal percentual = BigDecimal.valueOf(consumido / limite * 100);
        return new OrcamentoDTO(1L, categoria, BigDecimal.valueOf(limite), BigDecimal.valueOf(consumido), percentual, status);
    }

    private Gasto gastoComCategoria(CategoriaGasto categoria, double valor) {
        Gasto gasto = new Gasto();
        gasto.setCategoria(categoria);
        gasto.setValor(BigDecimal.valueOf(valor));
        gasto.setData(LocalDate.now());
        return gasto;
    }
}
