package com.claudio.financeiro.service;

import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class CalculoFinanceiroUtil {

    private CalculoFinanceiroUtil() {}

    static BigDecimal somarGastos(List<Gasto> gastos) {
        return gastos.stream()
                .map(Gasto::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal somarRendaTotal(List<Salario> salarios) {
        return salarios.stream()
                .map(s -> valorSeguro(s.getValor())
                        .add(valorSeguro(s.getComissao()))
                        .add(valorSeguro(s.getAdicional())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static Map<String, BigDecimal> agruparPorCategoria(List<Gasto> gastos) {
        return gastos.stream()
                .filter(g -> g.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        g -> g.getCategoria().name(),
                        Collectors.reducing(BigDecimal.ZERO, Gasto::getValor, BigDecimal::add)
                ));
    }

    static boolean salariosNoPeriodo(Salario s, Integer mes, Integer ano) {
        if (s.getData() == null) return false;
        boolean mesOk = (mes == null) || (s.getData().getMonthValue() == mes);
        boolean anoOk = (ano == null) || (s.getData().getYear() == ano);
        return mesOk && anoOk;
    }

    static Comparator<Gasto> ordenarPorDataDecrescente() {
        return (a, b) -> {
            LocalDate da = a.getData();
            LocalDate db = b.getData();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        };
    }

    private static BigDecimal valorSeguro(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
