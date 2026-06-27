package com.claudio.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO que representa uma transação no painel de resumo financeiro.
 *
 * Unifica gastos e entradas de salário num formato único para o frontend,
 * usando o campo "tipo" como discriminador:
 *   - "saida"  → gasto (valor sai da conta)
 *   - "entrada" → salário ou renda extra (valor entra na conta)
 *
 * Por que unificar?
 * O frontend exibe uma lista de transações recentes misturando entradas e saídas.
 * Ter um DTO unificado evita que o frontend precise lidar com dois formatos
 * diferentes de objetos na mesma lista.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransacaoDTO {

    private Long id;
    private String descricao;

    /**
     * Categoria do gasto (ex: "Alimentação"). Para entradas de salário, null.
     */
    private String categoria;

    private LocalDate data;
    private Double valor;

    /**
     * "entrada" para salários/rendas, "saida" para gastos.
     * O frontend usa este campo para decidir a cor (verde/vermelho) na exibição.
     */
    private String tipo;
}
