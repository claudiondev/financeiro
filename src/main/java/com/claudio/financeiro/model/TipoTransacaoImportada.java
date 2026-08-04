package com.claudio.financeiro.model;

/** Se uma transação do extrato vira Gasto ou Salário — decidido pelo sinal do valor no OFX. */
public enum TipoTransacaoImportada {
    GASTO,
    SALARIO
}
