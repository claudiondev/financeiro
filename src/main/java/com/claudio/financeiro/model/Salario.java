package com.claudio.financeiro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entrada de salário ou renda do usuário. Os campos comissao/adicional/descricao
 * são opcionais para acomodar salário fixo, comissão, hora extra, bônus etc.
 */
@Entity
@Table(name = "salarios")
public class Salario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    private BigDecimal valor;

    private BigDecimal comissao;
    private BigDecimal adicional;
    private String descricao;

    // ID único da transação no extrato bancário (OFX) — só preenchido em salários importados.
    private String fitid;

    @NotNull
    private LocalDate data;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public BigDecimal getComissao() { return comissao; }
    public void setComissao(BigDecimal comissao) { this.comissao = comissao; }

    public BigDecimal getAdicional() { return adicional; }
    public void setAdicional(BigDecimal adicional) { this.adicional = adicional; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFitid() { return fitid; }
    public void setFitid(String fitid) { this.fitid = fitid; }
}
