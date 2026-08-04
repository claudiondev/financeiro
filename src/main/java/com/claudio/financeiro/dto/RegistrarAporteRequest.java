package com.claudio.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Corpo de POST /metas-economia/{id}/aportes — vira um Gasto categoria Poupança por baixo. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarAporteRequest {

    @NotNull
    @Positive
    private BigDecimal valor;

    @NotNull
    private LocalDate data;
}
