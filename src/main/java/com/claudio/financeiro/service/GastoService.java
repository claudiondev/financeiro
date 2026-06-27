package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.GastoDTO;
import com.claudio.financeiro.dto.RelatorioMensalDTO;
import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.dto.TransacaoDTO;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GastoService {

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private SalarioRepository salarioRepository;

    // -------------------------------------------------------------------------
    // Operações básicas CRUD
    // -------------------------------------------------------------------------

    public Gasto salvar(Gasto gasto) {
        return gastoRepository.save(gasto);
    }

    public List<GastoDTO> listarPorUsuario(Long usuarioId) {
        return gastoRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Remove um gasto, mas só se ele pertencer ao usuário que está fazendo a requisição.
     *
     * Por que validar ownership no serviço e não no controller?
     * O serviço é a camada que conhece as regras de negócio. Se um dia existir
     * outro ponto de entrada (ex: agendamento, API interna), a proteção contra
     * IDOR continuará funcionando sem precisar duplicar a verificação.
     */
    public void deletar(Long id, Long usuarioId) {
        Gasto gasto = gastoRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Gasto não encontrado"));

        if (!gasto.getUsuario().getId().equals(usuarioId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Acesso negado");
        }

        gastoRepository.deleteById(id);
    }

    public List<GastoDTO> filtrarGastos(Long usuarioId, String categoria, Integer mes, Integer ano) {
        return gastoRepository.findByFiltros(usuarioId, categoria, mes, ano)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Dashboard — resumo financeiro geral
    // -------------------------------------------------------------------------

    /**
     * Calcula o resumo financeiro completo do usuário.
     *
     * Carregamos gastos e salários em memória e fazemos todos os cálculos
     * em uma única passagem — evitando N+1 queries ao banco.
     *
     * O método enriquece o ResumoMensal com:
     *   - maiorGasto: destaque no card principal do dashboard
     *   - categorias: dados para o gráfico de pizza
     *   - transacoesRecentes: últimas 5 movimentações para a timeline
     */
    public ResumoMensal calcularResumo(Long usuarioId) {
        List<Gasto> gastos = gastoRepository.findByUsuarioId(usuarioId);
        List<Salario> salarios = salarioRepository.findByUsuarioId(usuarioId);

        Double totalGastos = gastos.stream().mapToDouble(Gasto::getValor).sum();

        Double totalSalario = salarios.stream().mapToDouble(s ->
                valorSeguro(s.getValor()) +
                valorSeguro(s.getComissao()) +
                valorSeguro(s.getAdicional())
        ).sum();

        Double saldo = totalSalario - totalGastos;

        // Maior gasto individual (0.0 se não houver gastos)
        Double maiorGasto = gastos.stream()
                .mapToDouble(Gasto::getValor)
                .max()
                .orElse(0.0);

        // Agrupamento por categoria para o gráfico de pizza
        Map<String, Double> porCategoria = gastos.stream()
                .filter(g -> g.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        Gasto::getCategoria,
                        Collectors.summingDouble(Gasto::getValor)
                ));

        // Últimas 5 transações ordenadas por data decrescente
        List<TransacaoDTO> recentes = gastos.stream()
                .sorted(ordenarPorDataDecrescente())
                .limit(5)
                .map(g -> new TransacaoDTO(
                        g.getId(),
                        g.getDescricao(),
                        g.getCategoria(),
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

        if (saldo > 0) {
            resumo.setMensagem("Parabéns! Você economizou esse mês!");
        } else if (saldo < 0) {
            resumo.setMensagem("Atenção! Seus gastos ultrapassaram o salário!");
        }

        return resumo;
    }

    // -------------------------------------------------------------------------
    // Página de Relatórios — análise mensal por categoria
    // -------------------------------------------------------------------------

    /**
     * Gera o relatório mensal por categoria de gasto.
     *
     * @param usuarioId ID do usuário autenticado
     * @param mes       mês desejado (1-12), ou null para todos os meses
     * @param ano       ano desejado (ex: 2026), ou null para todos os anos
     */
    public RelatorioMensalDTO getRelatorio(Long usuarioId, Integer mes, Integer ano) {
        // Gastos filtrados por período via query JPQL parametrizada
        List<Gasto> gastosFiltrados = gastoRepository.findByFiltros(usuarioId, null, mes, ano);

        // Salários do período filtrados em memória (evita query extra no repositório)
        List<Salario> salariosDoMes = salarioRepository.findByUsuarioId(usuarioId)
                .stream()
                .filter(s -> salariosNoPeriodo(s, mes, ano))
                .collect(Collectors.toList());

        Double totalSaidas = gastosFiltrados.stream()
                .mapToDouble(Gasto::getValor)
                .sum();

        Double totalEntradas = salariosDoMes.stream()
                .mapToDouble(s -> valorSeguro(s.getValor()) +
                        valorSeguro(s.getComissao()) +
                        valorSeguro(s.getAdicional()))
                .sum();

        // Monta a lista de categorias com valor, percentual e média
        List<RelatorioMensalDTO.CategoriaDTO> categorias = gastosFiltrados.stream()
                .filter(g -> g.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        Gasto::getCategoria,
                        Collectors.summingDouble(Gasto::getValor)
                ))
                .entrySet().stream()
                .map(entry -> {
                    double percentual = totalSaidas > 0
                            ? (entry.getValue() / totalSaidas) * 100
                            : 0.0;
                    return new RelatorioMensalDTO.CategoriaDTO(
                            entry.getKey(),
                            entry.getValue(),
                            Math.round(percentual * 100.0) / 100.0,
                            entry.getValue() // média = valor do período (veja Javadoc da classe)
                    );
                })
                // Ordena da categoria com maior gasto para a menor
                .sorted(Comparator.comparingDouble(RelatorioMensalDTO.CategoriaDTO::getValor).reversed())
                .collect(Collectors.toList());

        return new RelatorioMensalDTO(categorias, totalEntradas, totalSaidas);
    }

    // -------------------------------------------------------------------------
    // Endpoints de categorias e resumo
    // -------------------------------------------------------------------------

    public Map<String, Double> resumoPorCategoria(Long usuarioId) {
        return gastoRepository.findByUsuarioId(usuarioId).stream()
                .filter(g -> g.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        Gasto::getCategoria,
                        Collectors.summingDouble(Gasto::getValor)
                ));
    }

    // -------------------------------------------------------------------------
    // Conversão Entidade → DTO (público para uso nos controllers)
    // -------------------------------------------------------------------------

    /**
     * Converte uma entidade Gasto para GastoDTO.
     *
     * Por que público?
     * O GastoController precisa chamar este método ao retornar o resultado
     * do POST /gastos. Mantemos a conversão no serviço (não no controller)
     * para que a lógica de mapeamento fique centralizada — se o DTO mudar,
     * só este método precisa ser atualizado.
     */
    public GastoDTO toDTO(Gasto gasto) {
        GastoDTO dto = new GastoDTO();
        dto.setId(gasto.getId());
        dto.setDescricao(gasto.getDescricao());
        dto.setValor(gasto.getValor());
        dto.setCategoria(gasto.getCategoria());
        dto.setData(gasto.getData());
        dto.setUsuarioId(gasto.getUsuario().getId());
        return dto;
    }

    // -------------------------------------------------------------------------
    // Métodos auxiliares privados
    // -------------------------------------------------------------------------

    /** Trata campos nullable (comissao, adicional) como zero quando ausentes. */
    private double valorSeguro(Double valor) {
        return valor != null ? valor : 0.0;
    }

    /**
     * Comparator que ordena gastos por data decrescente, tratando datas nulas
     * (que não podem ocorrer em produção por @NotNull, mas podem aparecer em testes).
     */
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

    /** Verifica se um salário se enquadra no período (mes/ano) solicitado. */
    private boolean salariosNoPeriodo(Salario s, Integer mes, Integer ano) {
        if (s.getData() == null) return false;
        boolean mesOk = (mes == null) || (s.getData().getMonthValue() == mes);
        boolean anoOk = (ano == null) || (s.getData().getYear() == ano);
        return mesOk && anoOk;
    }
}
