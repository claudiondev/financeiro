package com.claudio.financeiro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Gasto do usuário. As validações são aplicadas via @Valid no controller (400 automático). */
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
    private BigDecimal valor;

    @NotNull
    @Enumerated(EnumType.STRING)
    private CategoriaGasto categoria;

    @NotNull
    private LocalDate data;

    // Gasto avulso nasce true (comportamento atual, inalterado); só nasce false
    // quando gerado a partir de um GastoFixo (ver GastoFixoService).
    private boolean pago = true;

    @ManyToOne
    @JoinColumn(name = "gasto_fixo_id")
    private GastoFixo gastoFixo;

    // Controla o lembrete por e-mail: só reenvia se essa data for diferente de hoje.
    private LocalDate ultimoLembreteEnviadoEm;

    // Nulo em gastos antigos (criados antes desse campo existir) — a UI mostra "—".
    @Enumerated(EnumType.STRING)
    private FormaPagamento formaPagamento;

    // Agrupa as N parcelas de uma mesma compra parcelada; null quando não é parcelado.
    // UUID em vez de entidade própria: as parcelas nascem todas de uma vez, não há
    // "molde" que segue gerando (diferente de GastoFixo), então não compensa uma tabela.
    private String grupoParcelamento;

    private Integer numeroParcela;
    private Integer totalParcelas;

    // ID único da transação no extrato bancário (OFX) — só preenchido em gastos importados.
    // Garante que reimportar o mesmo período não duplica (ver ImportacaoService).
    private String fitid;

    // Preenchido quando esse gasto é um aporte pra uma meta de economia (categoria Poupança).
    // ON DELETE SET NULL: apagar a meta não apaga o histórico de gastos, só desvincula.
    @ManyToOne
    @JoinColumn(name = "meta_economia_id")
    private MetaEconomia metaEconomia;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public boolean isPago() { return pago; }
    public void setPago(boolean pago) { this.pago = pago; }

    public GastoFixo getGastoFixo() { return gastoFixo; }
    public void setGastoFixo(GastoFixo gastoFixo) { this.gastoFixo = gastoFixo; }

    public LocalDate getUltimoLembreteEnviadoEm() { return ultimoLembreteEnviadoEm; }
    public void setUltimoLembreteEnviadoEm(LocalDate ultimoLembreteEnviadoEm) { this.ultimoLembreteEnviadoEm = ultimoLembreteEnviadoEm; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public CategoriaGasto getCategoria() { return categoria; }
    public void setCategoria(CategoriaGasto categoria) { this.categoria = categoria; }

    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }

    public String getGrupoParcelamento() { return grupoParcelamento; }
    public void setGrupoParcelamento(String grupoParcelamento) { this.grupoParcelamento = grupoParcelamento; }

    public Integer getNumeroParcela() { return numeroParcela; }
    public void setNumeroParcela(Integer numeroParcela) { this.numeroParcela = numeroParcela; }

    public Integer getTotalParcelas() { return totalParcelas; }
    public void setTotalParcelas(Integer totalParcelas) { this.totalParcelas = totalParcelas; }

    public String getFitid() { return fitid; }
    public void setFitid(String fitid) { this.fitid = fitid; }

    public MetaEconomia getMetaEconomia() { return metaEconomia; }
    public void setMetaEconomia(MetaEconomia metaEconomia) { this.metaEconomia = metaEconomia; }
}
