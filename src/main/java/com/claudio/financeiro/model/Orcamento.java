package com.claudio.financeiro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Limite mensal de gasto por categoria — recorrente: vale todo mês até ser alterado pelo usuário. */
@Entity
@Table(name = "orcamentos", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "categoria"}))
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private CategoriaGasto categoria;

    @NotNull
    @Positive
    private BigDecimal limiteMensal;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CategoriaGasto getCategoria() { return categoria; }
    public void setCategoria(CategoriaGasto categoria) { this.categoria = categoria; }

    public BigDecimal getLimiteMensal() { return limiteMensal; }
    public void setLimiteMensal(BigDecimal limiteMensal) { this.limiteMensal = limiteMensal; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
