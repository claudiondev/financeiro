package com.claudio.financeiro.dto;

import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.TipoTransacaoImportada;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resposta de POST /importacao/ofx: uma transação lida do arquivo, ainda não salva. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransacaoImportadaDTO {
    private String fitid;
    private LocalDate data;
    private BigDecimal valor;
    private String descricao;
    private TipoTransacaoImportada tipo;
    // Null quando tipo = SALARIO (Salario não tem categoria). "OUTROS" por padrão em GASTO,
    // editável na tela de revisão antes de confirmar.
    private CategoriaGasto categoria;
    // Já existe um Gasto/Salario com esse fitid pro usuário — a tela mostra desmarcado.
    private boolean jaImportado;
}
