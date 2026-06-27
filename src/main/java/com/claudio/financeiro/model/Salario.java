package com.claudio.financeiro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Entidade que representa uma entrada de salário ou renda do usuário.
 *
 * Campos opcionais (comissao, adicional, descricao) permitem registrar
 * diferentes composições de renda: salário fixo, comissão de vendas,
 * adicionais de hora extra, bônus, etc.
 */
@Entity
@Table(name = "salarios")
public class Salario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    private Double valor;

    private Double comissao;
    private Double adicional;
    private String descricao;

    @NotNull
    private LocalDate data;

    /**
     * Relacionamento N:1 com Usuario.
     * Cada salário pertence a um único usuário.
     */
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Double getComissao() { return comissao; }
    public void setComissao(Double comissao) { this.comissao = comissao; }

    public Double getAdicional() { return adicional; }
    public void setAdicional(Double adicional) { this.adicional = adicional; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
