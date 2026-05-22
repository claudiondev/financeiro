package com.claudio.financeiro.service;
import com.claudio.financeiro.dto.GastoDTO;
import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    public void deletar(Long id) {
        gastoRepository.deleteById(id);
    }

    public List<GastoDTO> filtrarGastos(Long usuarioId, String categoria, Integer mes, Integer ano) {
        return gastoRepository.findByFiltros(usuarioId, categoria, mes, ano)
                .stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Double> resumoPorCategoria(Long usuarioId) {
        List<Gasto> gastos = gastoRepository.findByUsuarioId(usuarioId);
        return gastos.stream()
                .collect(Collectors.groupingBy(
                        Gasto::getCategoria,
                        Collectors.summingDouble(Gasto::getValor)
                ));
    }

    public ResumoMensal calcularResumo(Long usuarioId) {
        List<Gasto> gastos = gastoRepository.findByUsuarioId(usuarioId);
        List<Salario> salarios = salarioRepository.findByUsuarioId(usuarioId);
        Double totalGastos = gastos.stream().mapToDouble(Gasto::getValor).sum();
        Double totalSalario = salarios.stream().mapToDouble(s ->
                (s.getValor() != null ? s.getValor() : 0) +
                        (s.getComissao() != null ? s.getComissao() : 0) +
                        (s.getAdicional() != null ? s.getAdicional() : 0)
        ).sum();
        Double saldo = totalSalario - totalGastos;
        ResumoMensal resumo = new ResumoMensal();
        resumo.setTotalGasto(totalGastos);
        resumo.setTotalSalario(totalSalario);
        resumo.setSaldo(saldo);
        if (saldo > 0) {
            resumo.setMensagem("Parabéns! Você economizou esse mês!");
        } else if (saldo < 0) {
            resumo.setMensagem("Atenção! Seus gastos ultrapassaram o salário!");
        }
        return resumo;
    }

    private GastoDTO converterParaDTO(Gasto gasto) {
        GastoDTO dto = new GastoDTO();
        dto.setId(gasto.getId());
        dto.setDescricao(gasto.getDescricao());
        dto.setValor(gasto.getValor());
        dto.setCategoria(gasto.getCategoria());
        dto.setData(gasto.getData());
        dto.setUsuarioId(gasto.getUsuario().getId());
        return dto;
    }
}