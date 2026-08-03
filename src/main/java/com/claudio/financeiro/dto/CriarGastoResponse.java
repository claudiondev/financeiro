package com.claudio.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resposta de POST /gastos. avisoOrcamento vem preenchido só quando o gasto criado leva a
 * própria categoria a estourar ou chegar perto do limite do orçamento (Fase 6). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarGastoResponse {
    private GastoDTO gasto;
    private InsightDTO avisoOrcamento;
}
