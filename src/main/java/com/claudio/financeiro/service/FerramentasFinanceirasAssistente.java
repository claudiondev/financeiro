package com.claudio.financeiro.service;

import com.claudio.financeiro.model.CategoriaGasto;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FerramentasFinanceirasAssistente {

    private final ConsultaFinanceiraAssistenteService consultas;

    public FerramentasFinanceirasAssistente(ConsultaFinanceiraAssistenteService consultas) {
        this.consultas = consultas;
    }

    @Tool(description = "Consulta entradas, gastos pagos, saldo registrado e totais por categoria entre duas datas (AAAA-MM-DD).")
    public Map<String, Object> consultarResumoPeriodo(String inicio, String fim, ToolContext contexto) {
        return consultas.resumoPeriodo(usuario(contexto), data(inicio), data(fim));
    }

    @Tool(description = "Consulta total e até 20 maiores gastos pagos entre duas datas (AAAA-MM-DD). Categoria opcional: ALIMENTACAO, TRANSPORTE, LAZER, MORADIA, SAUDE, EDUCACAO, POUPANCA ou OUTROS.")
    public Map<String, Object> consultarGastosPeriodo(String inicio, String fim,
                                                       @ToolParam(required = false) String categoria,
                                                       ToolContext contexto) {
        CategoriaGasto filtro = null;
        if (categoria != null && !categoria.isBlank()) {
            try {
                filtro = CategoriaGasto.valueOf(categoria.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria inválida");
            }
        }
        return consultas.gastosPeriodo(usuario(contexto), data(inicio), data(fim), filtro);
    }

    @Tool(description = "Consulta os orçamentos cadastrados atualmente e o consumo pago de um mês e ano.")
    public Map<String, Object> consultarOrcamentos(int mes, int ano, ToolContext contexto) {
        return consultas.orcamentos(usuario(contexto), mes, ano);
    }

    private Long usuario(ToolContext contexto) {
        AtomicInteger chamadas = (AtomicInteger) contexto.getContext().get("chamadas");
        if (chamadas == null || chamadas.incrementAndGet() > 3) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de consultas por pergunta atingido");
        }
        return (Long) contexto.getContext().get("usuarioId");
    }

    private LocalDate data(String valor) {
        try {
            return LocalDate.parse(valor);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data inválida; use AAAA-MM-DD");
        }
    }
}
