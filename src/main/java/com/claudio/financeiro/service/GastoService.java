package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.CriarGastoRequest;
import com.claudio.financeiro.dto.EvolucaoMensalDTO;
import com.claudio.financeiro.dto.GastoDTO;
import com.claudio.financeiro.dto.RelatorioMensalDTO;
import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.dto.TransacaoDTO;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GastoService {

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private SalarioRepository salarioRepository;

    @Autowired
    private GastoFixoService gastoFixoService;

    public GastoDTO criar(CriarGastoRequest request, Usuario usuarioLogado) {
        Gasto gasto = paraEntidade(request, usuarioLogado);
        return toDTO(salvar(gasto));
    }

    public GastoDTO atualizar(Long id, CriarGastoRequest request, Long usuarioId) {
        Gasto gasto = buscarComOwnership(id, usuarioId);
        gasto.setDescricao(request.getDescricao());
        gasto.setValor(request.getValor());
        gasto.setCategoria(request.getCategoria());
        gasto.setData(request.getData());
        return toDTO(salvar(gasto));
    }

    public Gasto salvar(Gasto gasto) {
        return gastoRepository.save(gasto);
    }

    public List<GastoDTO> listarPorUsuario(Long usuarioId) {
        return gastoRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Verifica ownership antes de deletar, para evitar IDOR (usuário apagando gasto de outro)
    public void deletar(Long id, Long usuarioId) {
        buscarComOwnership(id, usuarioId);
        gastoRepository.deleteById(id);
    }

    public List<GastoDTO> filtrarGastos(Long usuarioId, CategoriaGasto categoria, Integer mes, Integer ano) {
        // Sem mês/ano não dá pra saber qual mês gerar — a listagem "todos os meses" não aciona a geração.
        if (mes != null && ano != null) {
            gastoFixoService.garantirGastosDoMesGerados(usuarioId, mes, ano);
        }

        // Mostra tudo, inclusive pendente — é aqui que o usuário vê e marca uma conta fixa como paga.
        return gastoRepository.findByFiltros(usuarioId, categoria, mes, ano)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ResumoMensal calcularResumo(Long usuarioId) {
        LocalDate hoje = LocalDate.now();
        gastoFixoService.garantirGastosDoMesGerados(usuarioId, hoje.getMonthValue(), hoje.getYear());

        // Só conta o que já foi pago — conta fixa pendente não desconta do saldo ainda.
        List<Gasto> gastos = gastoRepository.findByUsuarioId(usuarioId).stream()
                .filter(Gasto::isPago)
                .collect(Collectors.toList());
        List<Salario> salarios = salarioRepository.findByUsuarioId(usuarioId);

        BigDecimal totalGastos = somar(gastos, Gasto::getValor);
        BigDecimal totalSalario = somarRendaTotal(salarios);
        BigDecimal saldo = totalSalario.subtract(totalGastos);

        BigDecimal maiorGasto = gastos.stream()
                .map(Gasto::getValor)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        Map<String, BigDecimal> porCategoria = agruparPorCategoria(gastos);

        List<TransacaoDTO> recentes = gastos.stream()
                .sorted(ordenarPorDataDecrescente())
                .limit(5)
                .map(g -> new TransacaoDTO(
                        g.getId(),
                        g.getDescricao(),
                        g.getCategoria() != null ? g.getCategoria().name() : null,
                        g.getData(),
                        g.getValor(),
                        "saida"
                ))
                .collect(Collectors.toList());

        ResumoMensal resumo = new ResumoMensal();
        resumo.setTotalGasto(totalGastos);
        resumo.setTotalSalario(totalSalario);
        resumo.setSaldo(saldo);
        resumo.setMaiorGasto(maiorGasto);
        resumo.setCategorias(porCategoria);
        resumo.setTransacoesRecentes(recentes);

        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            resumo.setMensagem("Parabéns! Você economizou esse mês!");
        } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            resumo.setMensagem("Atenção! Seus gastos ultrapassaram o salário!");
        }

        return resumo;
    }

    /**
     * @param mes mês desejado (1-12), ou null para todos os meses
     * @param ano ano desejado, ou null para todos os anos
     */
    public RelatorioMensalDTO getRelatorio(Long usuarioId, Integer mes, Integer ano) {
        if (mes != null && ano != null) {
            gastoFixoService.garantirGastosDoMesGerados(usuarioId, mes, ano);
        }

        List<Gasto> gastosFiltrados = gastoRepository.findByFiltros(usuarioId, null, mes, ano).stream()
                .filter(Gasto::isPago)
                .collect(Collectors.toList());

        // Salários filtrados em memória (evita adicionar mais uma query no repositório)
        List<Salario> salariosDoMes = salarioRepository.findByUsuarioId(usuarioId)
                .stream()
                .filter(s -> salariosNoPeriodo(s, mes, ano))
                .collect(Collectors.toList());

        BigDecimal totalSaidas = somar(gastosFiltrados, Gasto::getValor);
        BigDecimal totalEntradas = somarRendaTotal(salariosDoMes);

        List<RelatorioMensalDTO.CategoriaDTO> categorias = gastosFiltrados.stream()
                .filter(g -> g.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        g -> g.getCategoria().name(),
                        Collectors.reducing(BigDecimal.ZERO, Gasto::getValor, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(entry -> new RelatorioMensalDTO.CategoriaDTO(
                        entry.getKey(),
                        entry.getValue(),
                        calcularPercentual(entry.getValue(), totalSaidas),
                        entry.getValue()
                ))
                .sorted(Comparator.comparing(RelatorioMensalDTO.CategoriaDTO::getValor).reversed())
                .collect(Collectors.toList());

        return new RelatorioMensalDTO(categorias, totalEntradas, totalSaidas);
    }

    public Map<String, BigDecimal> resumoPorCategoria(Long usuarioId) {
        return agruparPorCategoria(gastoRepository.findByUsuarioId(usuarioId));
    }

    /** Série histórica mês a mês (mais antigo primeiro), para o gráfico de evolução. */
    public List<EvolucaoMensalDTO> getEvolucaoMensal(Long usuarioId, int meses) {
        int mesesValidados = Math.max(1, Math.min(meses, 24));
        List<Salario> todosSalarios = salarioRepository.findByUsuarioId(usuarioId);
        List<EvolucaoMensalDTO> evolucao = new ArrayList<>();

        for (int i = mesesValidados - 1; i >= 0; i--) {
            YearMonth referencia = YearMonth.now().minusMonths(i);
            int mes = referencia.getMonthValue();
            int ano = referencia.getYear();
            gastoFixoService.garantirGastosDoMesGerados(usuarioId, mes, ano);

            List<Gasto> gastosDoMesPagos = gastoRepository.findByFiltros(usuarioId, null, mes, ano).stream()
                    .filter(Gasto::isPago)
                    .collect(Collectors.toList());
            BigDecimal totalSaidas = somar(gastosDoMesPagos, Gasto::getValor);
            BigDecimal totalEntradas = somarRendaTotal(
                    todosSalarios.stream().filter(s -> salariosNoPeriodo(s, mes, ano)).collect(Collectors.toList())
            );

            evolucao.add(new EvolucaoMensalDTO(mes, ano, totalEntradas, totalSaidas, totalEntradas.subtract(totalSaidas)));
        }

        return evolucao;
    }

    // Marca como pago um Gasto gerado a partir de um GastoFixo — é aqui que a conta
    // pendente passa a contar no saldo do mês.
    public GastoDTO marcarComoPago(Long id, Long usuarioId) {
        Gasto gasto = buscarComOwnership(id, usuarioId);
        gasto.setPago(true);
        return toDTO(salvar(gasto));
    }

    public GastoDTO toDTO(Gasto gasto) {
        GastoDTO dto = new GastoDTO();
        dto.setId(gasto.getId());
        dto.setDescricao(gasto.getDescricao());
        dto.setValor(gasto.getValor());
        dto.setCategoria(gasto.getCategoria());
        dto.setData(gasto.getData());
        dto.setUsuarioId(gasto.getUsuario().getId());
        dto.setPago(gasto.isPago());
        dto.setGastoFixoId(gasto.getGastoFixo() != null ? gasto.getGastoFixo().getId() : null);
        return dto;
    }

    private Gasto paraEntidade(CriarGastoRequest request, Usuario usuarioLogado) {
        Gasto gasto = new Gasto();
        gasto.setDescricao(request.getDescricao());
        gasto.setValor(request.getValor());
        gasto.setCategoria(request.getCategoria());
        gasto.setData(request.getData());
        gasto.setUsuario(usuarioLogado);
        return gasto;
    }

    private Gasto buscarComOwnership(Long id, Long usuarioId) {
        Gasto gasto = gastoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gasto não encontrado"));

        if (!gasto.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return gasto;
    }

    private Map<String, BigDecimal> agruparPorCategoria(List<Gasto> gastos) {
        return gastos.stream()
                .filter(g -> g.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        g -> g.getCategoria().name(),
                        Collectors.reducing(BigDecimal.ZERO, Gasto::getValor, BigDecimal::add)
                ));
    }

    private BigDecimal somar(List<Gasto> gastos, Function<Gasto, BigDecimal> extrator) {
        return gastos.stream()
                .map(extrator)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // comissao/adicional são opcionais no cadastro; tratamos null como 0
    private BigDecimal somarRendaTotal(List<Salario> salarios) {
        return salarios.stream()
                .map(s -> valorSeguro(s.getValor())
                        .add(valorSeguro(s.getComissao()))
                        .add(valorSeguro(s.getAdicional())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private BigDecimal calcularPercentual(BigDecimal valorCategoria, BigDecimal totalSaidas) {
        if (totalSaidas.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return valorCategoria
                .divide(totalSaidas, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Datas nulas não ocorrem em produção (@NotNull), mas podem aparecer em testes
    private Comparator<Gasto> ordenarPorDataDecrescente() {
        return (a, b) -> {
            LocalDate da = a.getData();
            LocalDate db = b.getData();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        };
    }

    private boolean salariosNoPeriodo(Salario s, Integer mes, Integer ano) {
        if (s.getData() == null) return false;
        boolean mesOk = (mes == null) || (s.getData().getMonthValue() == mes);
        boolean anoOk = (ano == null) || (s.getData().getYear() == ano);
        return mesOk && anoOk;
    }
}
