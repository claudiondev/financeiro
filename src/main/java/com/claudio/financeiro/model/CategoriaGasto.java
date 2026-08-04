package com.claudio.financeiro.model;

/**
 * Categorias fixas de gasto. Substitui o antigo campo de texto livre, que permitia
 * grafias inconsistentes ("Mercado" vs "mercado") e quebrava os agrupamentos de relatório.
 */
public enum CategoriaGasto {

    ALIMENTACAO("Alimentação"),
    TRANSPORTE("Transporte"),
    MORADIA("Moradia"),
    LAZER("Lazer"),
    SAUDE("Saúde"),
    EDUCACAO("Educação"),
    POUPANCA("Poupança"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaGasto(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
