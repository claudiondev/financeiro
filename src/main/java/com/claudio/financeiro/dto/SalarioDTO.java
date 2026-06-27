package com.claudio.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de resposta para operações de Salário.
 *
 * Por que existe este DTO?
 * A entidade Salario possui um campo @ManyToOne Usuario. Sem este DTO, serializar
 * um Salario incluiria o objeto Usuario completo na resposta JSON — expondo email,
 * status da conta e outros campos internos para o cliente.
 *
 * O DTO "achata" a resposta: em vez do objeto Usuario inteiro, retornamos apenas
 * o usuarioId, que é tudo que o frontend precisa para identificar o dono do registro.
 */
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

    /**
     * ID do usuário dono do registro.
     * Substituímos o objeto Usuario completo pelo seu identificador.
     */
    private Long usuarioId;
}
