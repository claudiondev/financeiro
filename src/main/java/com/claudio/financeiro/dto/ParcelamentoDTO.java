package com.claudio.financeiro.dto;

import com.claudio.financeiro.model.CategoriaGasto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resposta de GET /gastos/parcelamentos: uma compra parcelada ainda não quitada. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParcelamentoDTO {

    private String grupoParcelamento;
    private String descricao;
    private CategoriaGasto categoria;

    private BigDecimal valorTotal;

    /** Soma das parcelas que ainda vão vencer (a partir do mês corrente). */
    private BigDecimal valorRestante;

    /** Quantas parcelas já venceram (inclui a do mês corrente). */
    private Integer parcelasPagas;
    private Integer totalParcelas;

    /** Data da última parcela — quando o parcelamento termina. */
    private LocalDate ultimaParcela;
}
