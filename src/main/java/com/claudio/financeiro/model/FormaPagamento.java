package com.claudio.financeiro.model;

/** Como o gasto foi pago. Só CARTAO_CREDITO permite parcelamento (ver GastoService.criar). */
public enum FormaPagamento {
    DINHEIRO,
    PIX,
    CARTAO_CREDITO,
    CARTAO_DEBITO
}
