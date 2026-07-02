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

        Double maiorGasto = gastos.stream()
                .mapToDouble(Gasto::getValor)
                .max()
                .orElse(0.0);

        Map<String, Double> porCategoria = gastos.stream()
                .filter(g -> g.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        Gasto::getCategoria,
                        Collectors.summingDouble(Gasto::getValor)
                ));

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

    /**
     * @param mes mês desejado (1-12), ou null para todos os meses
     * @param ano ano desejado, ou null para todos os anos
     */
    public RelatorioMensalDTO getRelatorio(Long usuarioId, Integer mes, Integer ano) {
        List<Gasto> gastosFiltrados = gastoRepository.findByFiltros(usuarioId, null, mes, ano);

        // Salários filtrados em memória (evita adicionar mais uma query no repositório)
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
                            entry.getValue()
                    );
                })
                .sorted(Comparator.comparingDouble(RelatorioMensalDTO.CategoriaDTO::getValor).reversed())
                .collect(Collectors.toList());

        return new RelatorioMensalDTO(categorias, totalEntradas, totalSaidas);
    }

    public Map<String, Double> resumoPorCategoria(Long usuarioId) {
        return gastoRepository.findByUsuarioId(usuarioId).stream()
                .filter(g -> g.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        Gasto::getCategoria,
                        Collectors.summingDouble(Gasto::getValor)
                ));
    }

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

    // comissao/adicional são opcionais no cadastro; tratamos null como 0
    private double valorSeguro(Double valor) {
        return valor != null ? valor : 0.0;
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
