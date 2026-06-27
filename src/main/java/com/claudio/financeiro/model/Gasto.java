package com.claudio.financeiro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Entidade que representa um gasto do usuário.
 *
 * As anotações @NotBlank, @NotNull e @Positive são ativadas pelo @Valid no
 * controller e retornam 400 Bad Request automaticamente se violadas, sem
 * precisar de if/else manual no código de negócio.
 */
@Entity
@Table(name = "gastos")
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String descricao;

    @NotNull
    @Positive
    private Double valor;

    @NotBlank
    private String categoria;

    @NotNull
    private LocalDate data;

    /**
     * Relacionamento N:1 com Usuario.
     * Cada gasto pertence a um único usuário; um usuário pode ter muitos gastos.
     * A coluna "usuario_id" no banco é a chave estrangeira que materializa essa relação.
     */
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}
