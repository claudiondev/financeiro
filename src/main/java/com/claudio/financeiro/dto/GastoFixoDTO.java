package com.claudio.financeiro.dto;

import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.StatusGastoFixo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resposta de GET /gastos-fixos: o molde + status do gasto gerado no mes corrente. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GastoFixoDTO {

    private Long id;
    private CategoriaGasto categoria;
    private BigDecimal valor;
    private String descricao;
    private Integer diaVencimento;
    private LocalDate dataInicio;
    private boolean ativo;

    private StatusGastoFixo statusMesAtual;
    private LocalDate dataVencimentoMesAtual;

    /** Id do Gasto gerado esse mes (para o botão "marcar como pago" chamar PATCH /gastos/{id}/pagar). Null se ainda não foi gerado. */
    private Long gastoDoMesId;
}
