package com.claudio.financeiro.service;

import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Orcamento;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.OrcamentoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConsultaFinanceiraAssistenteService {

    private final GastoRepository gastoRepository;
    private final SalarioRepository salarioRepository;
    private final OrcamentoRepository orcamentoRepository;

    public ConsultaFinanceiraAssistenteService(GastoRepository gastoRepository,
                                               SalarioRepository salarioRepository,
                                               OrcamentoRepository orcamentoRepository) {
        this.gastoRepository = gastoRepository;
        this.salarioRepository = salarioRepository;
        this.orcamentoRepository = orcamentoRepository;
    }

    public Map<String, Object> resumoPeriodo(Long usuarioId, LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        List<Gasto> gastos = gastosPagos(usuarioId, inicio, fim, null);
        BigDecimal entradas = salarioRepository.findByUsuarioIdAndDataBetween(usuarioId, inicio, fim).stream()
                .map(s -> valor(s.getValor()).add(valor(s.getComissao())).add(valor(s.getAdicional())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saidas = CalculoFinanceiroUtil.somarGastos(gastos);
        return Map.of("inicio", inicio, "fim", fim, "entradas", entradas,
                "saidasPagas", saidas, "saldoRegistrado", entradas.subtract(saidas),
                "porCategoria", CalculoFinanceiroUtil.agruparPorCategoria(gastos));
    }

    public Map<String, Object> gastosPeriodo(Long usuarioId, LocalDate inicio, LocalDate fim,
                                              CategoriaGasto categoria) {
        validarPeriodo(inicio, fim);
        List<Gasto> gastos = gastosPagos(usuarioId, inicio, fim, categoria);
        List<Map<String, Object>> maiores = gastos.stream()
                .sorted(Comparator.comparing(Gasto::getValor).reversed().thenComparing(Gasto::getData))
                .limit(20)
                .map(g -> Map.<String, Object>of("descricao", g.getDescricao(), "valor", g.getValor(),
                        "data", g.getData(), "categoria", g.getCategoria().name()))
                .toList();
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("inicio", inicio);
        resposta.put("fim", fim);
        resposta.put("categoria", categoria == null ? "TODAS" : categoria.name());
        resposta.put("totalPago", CalculoFinanceiroUtil.somarGastos(gastos));
        resposta.put("quantidade", gastos.size());
        resposta.put("maioresGastos", maiores);
        resposta.put("detalhesLimitados", gastos.size() > maiores.size());
        return resposta;
    }

    public Map<String, Object> orcamentos(Long usuarioId, int mes, int ano) {
        if (mes < 1 || mes > 12 || ano < 2000 || ano > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mês ou ano inválido");
        }
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.plusMonths(1).minusDays(1);
        Map<CategoriaGasto, BigDecimal> consumo = gastosPagos(usuarioId, inicio, fim, null).stream()
                .collect(Collectors.groupingBy(Gasto::getCategoria,
                        Collectors.reducing(BigDecimal.ZERO, Gasto::getValor, BigDecimal::add)));
        List<Map<String, Object>> itens = orcamentoRepository.findByUsuarioId(usuarioId).stream()
                .map(o -> orcamentoComConsumo(o, consumo.getOrDefault(o.getCategoria(), BigDecimal.ZERO)))
                .toList();
        return Map.of("mes", mes, "ano", ano, "limitesSaoAtuais", true, "orcamentos", itens);
    }

    private Map<String, Object> orcamentoComConsumo(Orcamento orcamento, BigDecimal gasto) {
        return Map.of("categoria", orcamento.getCategoria().name(), "limiteAtual", orcamento.getLimiteMensal(),
                "gastoPago", gasto, "restante", orcamento.getLimiteMensal().subtract(gasto));
    }

    private List<Gasto> gastosPagos(Long usuarioId, LocalDate inicio, LocalDate fim, CategoriaGasto categoria) {
        return gastoRepository.findByUsuarioIdAndDataBetweenAndPagoTrue(usuarioId, inicio, fim).stream()
                .filter(g -> categoria == null || g.getCategoria() == categoria)
                .toList();
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)
                || ChronoUnit.MONTHS.between(inicio.withDayOfMonth(1), fim.withDayOfMonth(1)) >= 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um período válido de até 12 meses");
        }
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
