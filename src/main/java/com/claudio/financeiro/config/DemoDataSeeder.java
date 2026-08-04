package com.claudio.financeiro.config;

import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.FormaPagamento;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.GastoFixo;
import com.claudio.financeiro.model.Orcamento;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoFixoRepository;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.OrcamentoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import com.claudio.financeiro.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Cria, uma única vez, a conta compartilhada de demonstração ("Ver demo" no Login) com
 * dados de exemplo realistas — pra quem for avaliar o projeto (ex.: recrutador) ver o app
 * funcionando de imediato, sem precisar cadastro. Idempotente: roda em todo boot (local e
 * produção), mas só popula se a conta ainda não existir. DemoReadOnlyInterceptor garante
 * que ninguém consegue sujar esses dados por escrita via API.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    public static final String EMAIL_DEMO = "demo@meufinanceiro.app";

    private final UsuarioRepository usuarioRepository;
    private final GastoRepository gastoRepository;
    private final SalarioRepository salarioRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final GastoFixoRepository gastoFixoRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UsuarioRepository usuarioRepository, GastoRepository gastoRepository,
                          SalarioRepository salarioRepository, OrcamentoRepository orcamentoRepository,
                          GastoFixoRepository gastoFixoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.gastoRepository = gastoRepository;
        this.salarioRepository = salarioRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.gastoFixoRepository = gastoFixoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.findByEmail(EMAIL_DEMO).isPresent()) {
            return;
        }

        Usuario demo = new Usuario();
        demo.setEmail(EMAIL_DEMO);
        demo.setNome("Visitante Demo");
        // Ninguém loga com senha nessa conta (ver /auth/demo) — só precisa satisfazer o NOT NULL.
        demo.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));
        demo.setDemo(true);
        demo = usuarioRepository.save(demo);

        YearMonth mesAtual = YearMonth.now();
        popularSalarios(demo, mesAtual);
        popularGastosVariados(demo, mesAtual);
        popularParcelamento(demo, mesAtual);
        popularOrcamentos(demo);
        popularContasFixas(demo, mesAtual);

        log.info("Conta demo criada com dados de exemplo ({})", EMAIL_DEMO);
    }

    private void popularSalarios(Usuario demo, YearMonth mesAtual) {
        for (int i = 3; i >= 0; i--) {
            Salario salario = new Salario();
            salario.setUsuario(demo);
            salario.setValor(new BigDecimal("5000.00"));
            salario.setComissao(i == 1 ? new BigDecimal("300.00") : null);
            salario.setDescricao("Salário");
            salario.setData(mesAtual.minusMonths(i).atDay(5));
            salarioRepository.save(salario);
        }
    }

    private void popularGastosVariados(Usuario demo, YearMonth mesAtual) {
        // 3 meses anteriores: consumo estável, sem estourar nada — serve de base de
        // comparação pro Assistente (avaliarCategoriasEmAlta) e pro gráfico de Evolução.
        for (int i = 3; i >= 1; i--) {
            YearMonth mes = mesAtual.minusMonths(i);
            gasto(demo, mes.atDay(3), CategoriaGasto.ALIMENTACAO, "Supermercado", "620.00", FormaPagamento.PIX);
            gasto(demo, mes.atDay(18), CategoriaGasto.ALIMENTACAO, "Restaurante", "90.00", FormaPagamento.CARTAO_DEBITO);
            gasto(demo, mes.atDay(7), CategoriaGasto.TRANSPORTE, "Combustível", "260.00", FormaPagamento.CARTAO_DEBITO);
            gasto(demo, mes.atDay(10), CategoriaGasto.LAZER, "Cinema e streaming", "120.00", FormaPagamento.CARTAO_CREDITO);
            gasto(demo, mes.atDay(15), CategoriaGasto.SAUDE, "Farmácia", "140.00", FormaPagamento.DINHEIRO);
            gasto(demo, mes.atDay(20), CategoriaGasto.EDUCACAO, "Curso online", "110.00", FormaPagamento.CARTAO_CREDITO);
        }

        // Mês atual: dispara 2 regras do Assistente de propósito —
        // Lazer estoura o orçamento (140% do limite de R$250) e Alimentação sobe >20%
        // e mais de R$50 frente ao mês anterior (avaliarCategoriasEmAlta).
        gasto(demo, mesAtual.atDay(3), CategoriaGasto.ALIMENTACAO, "Supermercado", "690.00", FormaPagamento.PIX);
        gasto(demo, mesAtual.atDay(12), CategoriaGasto.ALIMENTACAO, "Restaurante", "150.00", FormaPagamento.CARTAO_DEBITO);
        gasto(demo, mesAtual.atDay(7), CategoriaGasto.TRANSPORTE, "Combustível", "270.00", FormaPagamento.CARTAO_DEBITO);
        gasto(demo, mesAtual.atDay(9), CategoriaGasto.LAZER, "Show e saídas", "230.00", FormaPagamento.CARTAO_CREDITO);
        gasto(demo, mesAtual.atDay(16), CategoriaGasto.LAZER, "Streaming e jogos", "120.00", FormaPagamento.CARTAO_CREDITO);
        gasto(demo, mesAtual.atDay(14), CategoriaGasto.SAUDE, "Farmácia", "95.00", FormaPagamento.DINHEIRO);
    }

    private void popularParcelamento(Usuario demo, YearMonth mesAtual) {
        String grupo = UUID.randomUUID().toString();
        int totalParcelas = 6;
        BigDecimal valorParcela = new BigDecimal("500.00");
        YearMonth primeiraParcela = mesAtual.minusMonths(2);

        for (int i = 0; i < totalParcelas; i++) {
            Gasto parcela = new Gasto();
            parcela.setUsuario(demo);
            parcela.setDescricao("Notebook novo");
            parcela.setValor(valorParcela);
            parcela.setCategoria(CategoriaGasto.OUTROS);
            parcela.setData(primeiraParcela.plusMonths(i).atDay(20));
            parcela.setFormaPagamento(FormaPagamento.CARTAO_CREDITO);
            parcela.setGrupoParcelamento(grupo);
            parcela.setNumeroParcela(i + 1);
            parcela.setTotalParcelas(totalParcelas);
            parcela.setPago(true);
            gastoRepository.save(parcela);
        }
    }

    private void popularOrcamentos(Usuario demo) {
        orcamento(demo, CategoriaGasto.LAZER, "250.00");       // estourado (mês atual ~350)
        orcamento(demo, CategoriaGasto.ALIMENTACAO, "1000.00"); // atenção (mês atual ~840, 84%)
        orcamento(demo, CategoriaGasto.TRANSPORTE, "400.00");   // dentro do limite (mês atual ~270)
    }

    private void popularContasFixas(Usuario demo, YearMonth mesAtual) {
        LocalDate hoje = LocalDate.now();
        int diaAluguel = clamp(hoje.getDayOfMonth() - 5);
        int diaInternet = clamp(hoje.getDayOfMonth());

        GastoFixo aluguel = contaFixa(demo, CategoriaGasto.MORADIA, "Aluguel", "1200.00", diaAluguel, mesAtual.minusMonths(6));
        GastoFixo internet = contaFixa(demo, CategoriaGasto.MORADIA, "Internet Fibra", "120.00", diaInternet, mesAtual.minusMonths(6));

        // Histórico dos 3 meses anteriores: sempre pago, só compõe os totais/gráficos.
        for (int i = 3; i >= 1; i--) {
            gastoDeContaFixa(aluguel, mesAtual.minusMonths(i).atDay(diaAluguel), true);
            gastoDeContaFixa(internet, mesAtual.minusMonths(i).atDay(diaInternet), true);
        }

        // Mês atual: aluguel vencido e não pago (ATRASADO), internet já paga (PAGO) —
        // mostra os dois estados mais relevantes da tela de Contas Fixas de cara.
        gastoDeContaFixa(aluguel, mesAtual.atDay(diaAluguel), false);
        gastoDeContaFixa(internet, mesAtual.atDay(diaInternet), true);
    }

    private void gasto(Usuario usuario, LocalDate data, CategoriaGasto categoria, String descricao,
                       String valor, FormaPagamento formaPagamento) {
        Gasto gasto = new Gasto();
        gasto.setUsuario(usuario);
        gasto.setData(data);
        gasto.setCategoria(categoria);
        gasto.setDescricao(descricao);
        gasto.setValor(new BigDecimal(valor));
        gasto.setFormaPagamento(formaPagamento);
        gasto.setPago(true);
        gastoRepository.save(gasto);
    }

    private void orcamento(Usuario usuario, CategoriaGasto categoria, String limiteMensal) {
        Orcamento orcamento = new Orcamento();
        orcamento.setUsuario(usuario);
        orcamento.setCategoria(categoria);
        orcamento.setLimiteMensal(new BigDecimal(limiteMensal));
        orcamentoRepository.save(orcamento);
    }

    private GastoFixo contaFixa(Usuario usuario, CategoriaGasto categoria, String descricao, String valor,
                                int diaVencimento, YearMonth dataInicio) {
        GastoFixo fixo = new GastoFixo();
        fixo.setUsuario(usuario);
        fixo.setCategoria(categoria);
        fixo.setDescricao(descricao);
        fixo.setValor(new BigDecimal(valor));
        fixo.setDiaVencimento(diaVencimento);
        fixo.setDataInicio(dataInicio.atDay(1));
        fixo.setAtivo(true);
        return gastoFixoRepository.save(fixo);
    }

    private void gastoDeContaFixa(GastoFixo fixo, LocalDate data, boolean pago) {
        Gasto gasto = new Gasto();
        gasto.setUsuario(fixo.getUsuario());
        gasto.setDescricao(fixo.getDescricao());
        gasto.setValor(fixo.getValor());
        gasto.setCategoria(fixo.getCategoria());
        gasto.setData(data);
        gasto.setGastoFixo(fixo);
        gasto.setPago(pago);
        gastoRepository.save(gasto);
    }

    private int clamp(int diaDoMes) {
        return Math.max(1, Math.min(diaDoMes, 28));
    }
}
