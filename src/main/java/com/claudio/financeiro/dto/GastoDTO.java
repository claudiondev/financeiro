package com.claudio.financeiro.dto;

import com.claudio.financeiro.model.CategoriaGasto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GastoDTO {
    private Long id;
    private String descricao;
    private BigDecimal valor;
    private CategoriaGasto categoria;
    private LocalDate data;
    private Long usuarioId;
    private boolean pago;

    /** Id do GastoFixo de origem, se esse gasto foi gerado automaticamente; null se for avulso. */
    private Long gastoFixoId;
}
