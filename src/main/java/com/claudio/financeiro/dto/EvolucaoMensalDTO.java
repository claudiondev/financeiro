package com.claudio.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Um ponto da série histórica de GET /gastos/evolucao: totais de um mês específico. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolucaoMensalDTO {

    private int mes;
    private int ano;
    private BigDecimal totalEntradas;
    private BigDecimal totalSaidas;
    private BigDecimal saldo;
}
