package com.claudio.financeiro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Molde de uma conta recorrente (aluguel, assinatura). Gera um Gasto novo todo mes, sob demanda. */
@Entity
@Table(name = "gastos_fixos")
public class GastoFixo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private CategoriaGasto categoria;

    @NotNull
    @Positive
    private BigDecimal valor;

    @NotBlank
    private String descricao;

    @NotNull
    @Min(1)
    @Max(31)
    private Integer diaVencimento;

    @NotNull
    private LocalDate dataInicio;

    private boolean ativo = true;

    // Null = recorrente pra sempre (aluguel, assinatura — comportamento original). Preenchido
    // = tem fim (financiamento/dívida): a geração automática para sozinha ao atingir esse
    // número de parcelas já criadas (ver GastoFixoService.garantirGastosDoMesGerados).
    private Integer totalParcelas;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CategoriaGasto getCategoria() { return categoria; }
    public void setCategoria(CategoriaGasto categoria) { this.categoria = categoria; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Integer getDiaVencimento() { return diaVencimento; }
    public void setDiaVencimento(Integer diaVencimento) { this.diaVencimento = diaVencimento; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public Integer getTotalParcelas() { return totalParcelas; }
    public void setTotalParcelas(Integer totalParcelas) { this.totalParcelas = totalParcelas; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
