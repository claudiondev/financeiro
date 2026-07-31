package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.EvolucaoMensalDTO;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvolucaoService {

    private final GastoRepository gastoRepository;
    private final SalarioRepository salarioRepository;
    private final GastoFixoService gastoFixoService;

    public EvolucaoService(GastoRepository gastoRepository, SalarioRepository salarioRepository,
                           GastoFixoService gastoFixoService) {
        this.gastoRepository = gastoRepository;
        this.salarioRepository = salarioRepository;
        this.gastoFixoService = gastoFixoService;
    }

    public List<EvolucaoMensalDTO> getEvolucaoMensal(Long usuarioId, int meses) {
        int mesesValidados = Math.max(1, Math.min(meses, 24));
        YearMonth mesAtual = YearMonth.now();
        LocalDate desde = mesAtual.minusMonths(mesesValidados - 1).atDay(1);

        for (int i = mesesValidados - 1; i >= 0; i--) {
            YearMonth ref = mesAtual.minusMonths(i);
            gastoFixoService.garantirGastosDoMesGerados(usuarioId, ref.getMonthValue(), ref.getYear());
        }

        Map<YearMonth, BigDecimal> gastosPorMes = toMap(
                gastoRepository.somarGastosPagosAgrupadoPorMes(usuarioId, desde));
        Map<YearMonth, BigDecimal> rendaPorMes = toMap(
                salarioRepository.somarRendaAgrupadaPorMes(usuarioId, desde));

        List<EvolucaoMensalDTO> evolucao = new ArrayList<>();
        for (int i = mesesValidados - 1; i >= 0; i--) {
            YearMonth ref = mesAtual.minusMonths(i);
            BigDecimal saidas = gastosPorMes.getOrDefault(ref, BigDecimal.ZERO);
            BigDecimal entradas = rendaPorMes.getOrDefault(ref, BigDecimal.ZERO);
            evolucao.add(new EvolucaoMensalDTO(
                    ref.getMonthValue(), ref.getYear(),
                    entradas, saidas, entradas.subtract(saidas)));
        }

        return evolucao;
    }

    private Map<YearMonth, BigDecimal> toMap(List<Object[]> rows) {
        Map<YearMonth, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            int ano = ((Number) row[0]).intValue();
            int mes = ((Number) row[1]).intValue();
            BigDecimal total = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            map.put(YearMonth.of(ano, mes), total);
        }
        return map;
    }
}
