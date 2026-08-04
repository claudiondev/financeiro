package com.claudio.financeiro.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê transações de um arquivo OFX (extrato bancário). Parser próprio, sem lib externa:
 * bancos brasileiros costumam exportar OFX 1.x, que é SGML "solto" (tags sem fechamento,
 * ex.: {@code <FITID>123}), não XML válido — uma lib de XML genérica rejeitaria o arquivo.
 * A mesma expressão captura tanto esse formato quanto o XML fechado (OFX 2.x), porque para
 * de capturar no primeiro '<' ou quebra de linha, o que vier primeiro.
 */
@Component
public class OfxParser {

    private static final Pattern BLOCO_TRANSACAO =
            Pattern.compile("<STMTTRN>(.*?)</STMTTRN>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<TransacaoOfx> parse(String conteudo) {
        Matcher blocos = BLOCO_TRANSACAO.matcher(conteudo);
        List<TransacaoOfx> transacoes = new ArrayList<>();

        while (blocos.find()) {
            String bloco = blocos.group(1);
            TransacaoOfx transacao = parseTransacao(bloco);
            if (transacao != null) {
                transacoes.add(transacao);
            }
        }

        if (transacoes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nenhuma transação encontrada — confira se o arquivo é um extrato OFX válido");
        }

        return transacoes;
    }

    // Uma transação sem valor, data ou fitid não tem como ser processada com segurança —
    // melhor pular ela do que quebrar a importação inteira por causa de uma linha estranha.
    private TransacaoOfx parseTransacao(String bloco) {
        String fitid = campo(bloco, "FITID");
        String dataBruta = campo(bloco, "DTPOSTED");
        String valorBruto = campo(bloco, "TRNAMT");
        String descricao = campo(bloco, "NAME");
        if (descricao == null || descricao.isBlank()) {
            descricao = campo(bloco, "MEMO");
        }

        if (fitid == null || dataBruta == null || valorBruto == null) {
            return null;
        }

        LocalDate data = LocalDate.parse(dataBruta.substring(0, 8), FORMATO_DATA);
        BigDecimal valor = new BigDecimal(valorBruto.trim());
        return new TransacaoOfx(fitid.trim(), data, valor,
                descricao != null && !descricao.isBlank() ? descricao.trim() : "Transação importada");
    }

    private String campo(String bloco, String tag) {
        Matcher m = Pattern.compile("<" + tag + ">\\s*([^<\r\n]*)", Pattern.CASE_INSENSITIVE).matcher(bloco);
        return m.find() ? m.group(1) : null;
    }
}
