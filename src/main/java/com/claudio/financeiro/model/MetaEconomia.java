package com.claudio.financeiro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Meta de juntar dinheiro (viagem, reserva de emergência, etc.) — o oposto de Orcamento,
 * que é um teto de gasto. Progresso não é armazenado aqui: é a soma dos Gasto vinculados
 * a essa meta (categoria Poupança), calculada ao vivo em MetaEconomiaService.
 */
@Entity
@Table(name = "metas_economia")
public class MetaEconomia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    @NotNull
    @Positive
    private BigDecimal valorAlvo;

    // Opcional — meta sem pressa de data só acompanha percentual concluído, sem status ATRASADA.
    private LocalDate prazo;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getValorAlvo() { return valorAlvo; }
    public void setValorAlvo(BigDecimal valorAlvo) { this.valorAlvo = valorAlvo; }

    public LocalDate getPrazo() { return prazo; }
    public void setPrazo(LocalDate prazo) { this.prazo = prazo; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
