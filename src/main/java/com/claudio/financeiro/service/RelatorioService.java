package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.RelatorioMensalDTO;
import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.dto.TransacaoDTO;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    private final GastoRepository gastoRepository;
    private final SalarioRepository salarioRepository;
    private final GastoFixoService gastoFixoService;

    public RelatorioService(GastoRepository gastoRepository, SalarioRepository salarioRepository,
                            GastoFixoService gastoFixoService) {
        this.gastoRepository = gastoRepository;
        this.salarioRepository = salarioRepository;
        this.gastoFixoService = gastoFixoService;
    }

    public ResumoMensal calcularResumo(Long usuarioId) {
        LocalDate hoje = LocalDate.now();
        int mes = hoje.getMonthValue();
        int ano = hoje.getYear();
        gastoFixoService.garantirGastosDoMesGerados(usuarioId, mes, ano);

        List<Gasto> gastos = gastoRepository.findByFiltros(usuarioId, null, mes, ano).stream()
                .filter(Gasto::isPago)
                .collect(Collectors.toList());
        List<Salario> salarios = salarioRepository.findByUsuarioId(usuarioId).stream()
                .filter(s -> CalculoFinanceiroUtil.salariosNoPeriodo(s, mes, ano))
                .collect(Collectors.toList());

        BigDecimal totalGastos = CalculoFinanceiroUtil.somarGastos(gastos);
        BigDecimal totalSalario = CalculoFinanceiroUtil.somarRendaTotal(salarios);
        BigDecimal saldo = totalSalario.subtract(totalGastos);

        BigDecimal maiorGasto = gastos.stream()
                .map(Gasto::getValor)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        Map<String, BigDecimal> porCategoria = CalculoFinanceiroUtil.agruparPorCategoria(gastos);

        List<TransacaoDTO> recentes = gastos.stream()
                .sorted(CalculoFinanceiroUtil.ordenarPorDataDecrescente())
                .limit(5)
                .map(g -> new TransacaoDTO(
                        g.getId(),
                        g.getDescricao(),
                        g.getCategoria() != null ? g.getCategoria().name() : null,
                        g.getData(),
                        g.getValor(),
                        "saida"
                ))
                .collect(Collectors.toList());

        ResumoMensal resumo = new ResumoMensal();
        resumo.setTotalGasto(totalGastos);
        resumo.setTotalSalario(totalSalario);
        resumo.setSaldo(saldo);
        resumo.setMaiorGasto(maiorGasto);
        resumo.setCategorias(porCategoria);
        resumo.setTransacoesRecentes(recentes);

        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            resumo.setMensagem("Parabéns! Você economizou esse mês!");
        } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            resumo.setMensagem("Atenção! Seus gastos ultrapassaram o salário!");
        }

        return resumo;
    }

    public RelatorioMensalDTO getRelatorio(Long usuarioId, Integer mes, Integer ano) {
        if (mes != null && ano != null) {
            gastoFixoService.garantirGastosDoMesGerados(usuarioId, mes, ano);
        }

        List<Gasto> gastosFiltrados = gastoRepository.findByFiltros(usuarioId, null, mes, ano).stream()
                .filter(Gasto::isPago)
                .collect(Collectors.toList());

        List<Salario> salariosDoMes = salarioRepository.findByUsuarioId(usuarioId)
                .stream()
                .filter(s -> CalculoFinanceiroUtil.salariosNoPeriodo(s, mes, ano))
                .collect(Collectors.toList());

        BigDecimal totalSaidas = CalculoFinanceiroUtil.somarGastos(gastosFiltrados);
        BigDecimal totalEntradas = CalculoFinanceiroUtil.somarRendaTotal(salariosDoMes);

        List<RelatorioMensalDTO.CategoriaDTO> categorias = gastosFiltrados.stream()
                .filter(g -> g.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        g -> g.getCategoria().name(),
                        Collectors.reducing(BigDecimal.ZERO, Gasto::getValor, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(entry -> new RelatorioMensalDTO.CategoriaDTO(
                        entry.getKey(),
                        entry.getValue(),
                        calcularPercentual(entry.getValue(), totalSaidas),
                        entry.getValue()
                ))
                .sorted(Comparator.comparing(RelatorioMensalDTO.CategoriaDTO::getValor).reversed())
                .collect(Collectors.toList());

        return new RelatorioMensalDTO(categorias, totalEntradas, totalSaidas);
    }

    public Map<String, BigDecimal> resumoPorCategoria(Long usuarioId) {
        return CalculoFinanceiroUtil.agruparPorCategoria(gastoRepository.findByUsuarioId(usuarioId));
    }

    private BigDecimal calcularPercentual(BigDecimal valorCategoria, BigDecimal totalSaidas) {
        if (totalSaidas.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return valorCategoria
                .divide(totalSaidas, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
