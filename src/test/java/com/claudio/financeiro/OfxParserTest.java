package com.claudio.financeiro;

import com.claudio.financeiro.service.OfxParser;
import com.claudio.financeiro.service.TransacaoOfx;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfxParserTest {

    private final OfxParser parser = new OfxParser();

    @Test
    void deveLerTransacoesNoFormatoSgmlSemFechamento() {
        String ofx = """
                <OFX>
                <BANKTRANLIST>
                <STMTTRN>
                <TRNTYPE>DEBIT
                <DTPOSTED>20260615120000[-3:BRT]
                <TRNAMT>-45.90
                <FITID>202606150001
                <MEMO>PIX ENVIADO FULANO
                </STMTTRN>
                <STMTTRN>
                <TRNTYPE>CREDIT
                <DTPOSTED>20260601080000[-3:BRT]
                <TRNAMT>5000.00
                <FITID>202606010001
                <NAME>SALARIO EMPRESA XYZ
                </STMTTRN>
                </BANKTRANLIST>
                </OFX>
                """;

        List<TransacaoOfx> transacoes = parser.parse(ofx);

        assertEquals(2, transacoes.size());

        TransacaoOfx debito = transacoes.get(0);
        assertEquals("202606150001", debito.getFitid());
        assertEquals(LocalDate.of(2026, 6, 15), debito.getData());
        assertEquals(0, new BigDecimal("-45.90").compareTo(debito.getValor()));
        assertEquals("PIX ENVIADO FULANO", debito.getDescricao());

        TransacaoOfx credito = transacoes.get(1);
        assertEquals("202606010001", credito.getFitid());
        assertEquals(0, new BigDecimal("5000.00").compareTo(credito.getValor()));
        assertEquals("SALARIO EMPRESA XYZ", credito.getDescricao());
    }

    @Test
    void deveLerTransacoesNoFormatoXmlFechado() {
        String ofx = """
                <STMTTRN>
                <TRNTYPE>DEBIT</TRNTYPE>
                <DTPOSTED>20260610</DTPOSTED>
                <TRNAMT>-120.50</TRNAMT>
                <FITID>abc123</FITID>
                <MEMO>SUPERMERCADO</MEMO>
                </STMTTRN>
                """;

        List<TransacaoOfx> transacoes = parser.parse(ofx);

        assertEquals(1, transacoes.size());
        assertEquals("abc123", transacoes.get(0).getFitid());
        assertEquals(LocalDate.of(2026, 6, 10), transacoes.get(0).getData());
    }

    @Test
    void devePreferirNameSobreMemoQuandoAmbosExistem() {
        String ofx = """
                <STMTTRN>
                <DTPOSTED>20260610
                <TRNAMT>-10.00
                <FITID>1
                <NAME>NOME PRIORITARIO
                <MEMO>MEMO SECUNDARIO
                </STMTTRN>
                """;

        List<TransacaoOfx> transacoes = parser.parse(ofx);

        assertEquals("NOME PRIORITARIO", transacoes.get(0).getDescricao());
    }

    @Test
    void devePularTransacaoSemFitidSemQuebrarAsDemais() {
        String ofx = """
                <STMTTRN>
                <DTPOSTED>20260610
                <TRNAMT>-10.00
                </STMTTRN>
                <STMTTRN>
                <DTPOSTED>20260611
                <TRNAMT>-20.00
                <FITID>valido1
                </STMTTRN>
                """;

        List<TransacaoOfx> transacoes = parser.parse(ofx);

        assertEquals(1, transacoes.size());
        assertEquals("valido1", transacoes.get(0).getFitid());
    }

    @Test
    void deveLancarBadRequestQuandoArquivoNaoTemTransacaoNenhuma() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> parser.parse("isso nao é um arquivo OFX"));

        assertEquals(400, ex.getStatusCode().value());
    }
}
