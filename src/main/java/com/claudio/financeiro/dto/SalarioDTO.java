package com.claudio.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Resposta de operações de Salário — substitui o objeto Usuario completo por usuarioId. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalarioDTO {

    private Long id;
    private Double valor;
    private Double comissao;
    private Double adicional;
    private String descricao;
    private LocalDate data;
    private Long usuarioId;
}
