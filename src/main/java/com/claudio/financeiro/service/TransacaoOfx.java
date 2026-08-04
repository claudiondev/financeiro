package com.claudio.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma transação lida de um arquivo OFX, antes de virar Gasto ou Salário.
 * Valor mantém o sinal original do banco (negativo = saída, positivo = entrada) —
 * quem decide o que fazer com isso é o ImportacaoService.
 */
public class TransacaoOfx {

    private final String fitid;
    private final LocalDate data;
    private final BigDecimal valor;
    private final String descricao;

    public TransacaoOfx(String fitid, LocalDate data, BigDecimal valor, String descricao) {
        this.fitid = fitid;
        this.data = data;
        this.valor = valor;
        this.descricao = descricao;
    }

    public String getFitid() { return fitid; }
    public LocalDate getData() { return data; }
    public BigDecimal getValor() { return valor; }
    public String getDescricao() { return descricao; }
}
