package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.InsightDTO;
import com.claudio.financeiro.dto.OrcamentoDTO;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Severidade;
import com.claudio.financeiro.model.StatusOrcamento;
import com.claudio.financeiro.model.TipoInsight;
import com.claudio.financeiro.repository.GastoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Motor de regras determinístico do Assistente financeiro (Fase 6) — sem LLM, ver GeradorDeInsight.
 * Combina 4 fontes de insight e devolve as 5 mais relevantes, ordenadas por severidade.
 */
@Service
public class AssistenteService implements GeradorDeInsight {

    private static final BigDecimal CRESCIMENTO_MINIMO_PERCENTUAL = BigDecimal.valueOf(20);
    private static final BigDecimal AUMENTO_MINIMO_ABSOLUTO = BigDecimal.valueOf(50);
    private static final int MAXIMO_INSIGHTS = 5;
    private static final int MAXIMO_DICAS = 2;

    // Mesmo formato pt-BR (R$ 1.234,56) que o frontend usa via Intl.NumberFormat — sem isso,
    // as mensagens do Assistente saíam com separador decimal americano ("R$ 1240.00").
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private final OrcamentoService orcamentoService;
    private final GastoRepository gastoRepository;

    public AssistenteService(OrcamentoService orcamentoService, GastoRepository gastoRepository) {
        this.orcamentoService = orcamentoService;
        this.gastoRepository = gastoRepository;
    }

    @Override
    public List<InsightDTO> gerarInsights(Long usuarioId) {
        LocalDate hoje = LocalDate.now();
        List<OrcamentoDTO> orcamentosComConsumo =
                orcamentoService.listarComConsumo(usuarioId, hoje.getMonthValue(), hoje.getYear());

        Map<CategoriaGasto, BigDecimal> totalPorCategoriaAtual = totalPorCategoria(usuarioId, hoje.getMonthValue(), hoje.getYear());

        List<InsightDTO> insights = new ArrayList<>();
        insights.addAll(avaliarOrcamentos(orcamentosComConsumo));
        avaliarRitmoDiario(orcamentosComConsumo, hoje).ifPresent(insights::add);
        insights.addAll(avaliarCategoriasEmAlta(usuarioId, totalPorCategoriaAtual, hoje));
        insights.addAll(selecionarDicasEducacionais(totalPorCategoriaAtual));

        return insights.stream()
                .sorted(Comparator.comparing(InsightDTO::getSeveridade))
                .limit(MAXIMO_INSIGHTS)
                .collect(Collectors.toList());
    }

    private List<InsightDTO> avaliarOrcamentos(List<OrcamentoDTO> orcamentosComConsumo) {
        List<InsightDTO> insights = new ArrayList<>();
        for (OrcamentoDTO orcamento : orcamentosComConsumo) {
            if (orcamento.getStatus() == StatusOrcamento.ESTOURADO) {
                insights.add(insightDeOrcamento(orcamento, TipoInsight.ORCAMENTO_ESTOURADO, Severidade.CRITICO, "Orçamento estourado"));
            } else if (orcamento.getStatus() == StatusOrcamento.ATENCAO) {
                insights.add(insightDeOrcamento(orcamento, TipoInsight.ORCAMENTO_ATENCAO, Severidade.ATENCAO, "Orçamento quase no limite"));
            }
        }
        return insights;
    }

    private InsightDTO insightDeOrcamento(OrcamentoDTO orcamento, TipoInsight tipo, Severidade severidade, String titulo) {
        return new InsightDTO(tipo, severidade, orcamento.getCategoria(), titulo, String.format(
                "Você já consumiu %s%% do orçamento de %s este mês.",
                orcamento.getPercentualConsumido().setScale(0, RoundingMode.HALF_UP),
                orcamento.getCategoria().getDescricao()
        ));
    }

    // Só avalia se houver ao menos uma meta cadastrada — sem orçamento, não há teto pra projetar contra.
    private Optional<InsightDTO> avaliarRitmoDiario(List<OrcamentoDTO> orcamentosComConsumo, LocalDate hoje) {
        if (orcamentosComConsumo.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal totalGastoAteHoje = orcamentosComConsumo.stream()
                .map(OrcamentoDTO::getValorConsumido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal somaOrcamentos = orcamentosComConsumo.stream()
                .map(OrcamentoDTO::getLimiteMensal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal projecao = totalGastoAteHoje
                .divide(BigDecimal.valueOf(hoje.getDayOfMonth()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(hoje.lengthOfMonth()));

        if (projecao.compareTo(somaOrcamentos) <= 0) {
            return Optional.empty();
        }

        return Optional.of(new InsightDTO(
                TipoInsight.RITMO_ACIMA_DO_ORCAMENTO, Severidade.CRITICO, null,
                "Ritmo de gastos acima do planejado",
                String.format(
                        "No ritmo atual, você deve fechar o mês em torno de %s, acima da soma dos seus orçamentos (%s).",
                        MOEDA.format(projecao), MOEDA.format(somaOrcamentos)
                )
        ));
    }

    // Crescimento >=20% E aumento absoluto >=R$50 — evita alarme falso em categoria pequena.
    private List<InsightDTO> avaliarCategoriasEmAlta(Long usuarioId, Map<CategoriaGasto, BigDecimal> totalPorCategoriaAtual, LocalDate hoje) {
        YearMonth mesAnterior = YearMonth.from(hoje).minusMonths(1);
        Map<CategoriaGasto, BigDecimal> totalPorCategoriaAnterior =
                totalPorCategoria(usuarioId, mesAnterior.getMonthValue(), mesAnterior.getYear());

        List<InsightDTO> insights = new ArrayList<>();
        for (Map.Entry<CategoriaGasto, BigDecimal> entry : totalPorCategoriaAtual.entrySet()) {
            BigDecimal anterior = totalPorCategoriaAnterior.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            if (anterior.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal atual = entry.getValue();
            BigDecimal aumento = atual.subtract(anterior);
            BigDecimal crescimentoPercentual = aumento
                    .divide(anterior, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (crescimentoPercentual.compareTo(CRESCIMENTO_MINIMO_PERCENTUAL) >= 0
                    && aumento.compareTo(AUMENTO_MINIMO_ABSOLUTO) >= 0) {
                CategoriaGasto categoria = entry.getKey();
                insights.add(new InsightDTO(
                        TipoInsight.CATEGORIA_EM_ALTA, Severidade.ATENCAO, categoria,
                        "Categoria em alta",
                        String.format(
                                "Seus gastos com %s subiram %s%% em relação ao mês passado (%s a mais).",
                                categoria.getDescricao(),
                                crescimentoPercentual.setScale(0, RoundingMode.HALF_UP),
                                MOEDA.format(aumento)
                        )
                ));
            }
        }
        return insights;
    }

    // Dica das 1-2 categorias com maior gasto no mês — funciona mesmo sem nenhuma meta cadastrada.
    private List<InsightDTO> selecionarDicasEducacionais(Map<CategoriaGasto, BigDecimal> totalPorCategoriaAtual) {
        return totalPorCategoriaAtual.entrySet().stream()
                .sorted(Map.Entry.<CategoriaGasto, BigDecimal>comparingByValue().reversed())
                .limit(MAXIMO_DICAS)
                .map(entry -> {
                    CategoriaGasto categoria = entry.getKey();
                    return new InsightDTO(
                            TipoInsight.DICA_EDUCACIONAL, Severidade.INFO, categoria,
                            "Dica para " + categoria.getDescricao(),
                            DicasEducacionaisCatalogo.obterDica(categoria)
                    );
                })
                .collect(Collectors.toList());
    }

    private Map<CategoriaGasto, BigDecimal> totalPorCategoria(Long usuarioId, int mes, int ano) {
        List<Gasto> gastosDoMes = gastoRepository.findByFiltros(usuarioId, null, mes, ano).stream()
                .filter(Gasto::isPago)
                .collect(Collectors.toList());

        return gastosDoMes.stream()
                .filter(g -> g.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        Gasto::getCategoria,
                        Collectors.reducing(BigDecimal.ZERO, Gasto::getValor, BigDecimal::add)
                ));
    }
}
