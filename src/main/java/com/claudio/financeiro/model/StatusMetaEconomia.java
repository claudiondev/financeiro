package com.claudio.financeiro.model;

/** Status calculado de uma meta de economia — nunca armazenado, sempre derivado do progresso. */
public enum StatusMetaEconomia {
    EM_ANDAMENTO,
    CONCLUIDA,
    // Tem prazo definido, o prazo já passou e o valor-alvo ainda não foi atingido.
    ATRASADA
}
