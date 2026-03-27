package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GastoService {

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private SalarioRepository salarioRepository;

    public Gasto salvar(Gasto gasto) {
        return gastoRepository.save(gasto);
    }

    // Alterado: Agora lista apenas os gastos do dono da conta
    public List<Gasto> listarPorUsuario(Long usuarioId) {
        return gastoRepository.findByUsuarioId(usuarioId);
    }

    public void deletar(Long id) {
        gastoRepository.deleteById(id);
    }

    // Alterado: O resumo agora recebe o ID do usuário logado
    public ResumoMensal calcularResumo(Long usuarioId) {
        // Busca apenas os dados vinculados ao ID do usuário
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
            resumo.setMensagem("Parabéns! Você economizou esse mês! 🎉");
        } else if (saldo < 0) {
            resumo.setMensagem("Atenção! Seus gastos ultrapassaram o salário!");
        }

        return resumo;
    }
}