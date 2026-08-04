package com.claudio.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resposta de POST /importacao/confirmar. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportacaoResultadoDTO {
    private int gastosCriados;
    private int salariosCriados;
}
