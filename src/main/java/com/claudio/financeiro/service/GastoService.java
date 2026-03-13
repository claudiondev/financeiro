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
    public Gasto salvar  (Gasto gasto) { return gastoRepository.save(gasto);
    }
    public List<Gasto> listar() { return gastoRepository.findAll();
    }

    public void deletar(Long id) {
        gastoRepository.deleteById(id);
    }

    @Autowired
    private SalarioRepository salarioRepository;
        public ResumoMensal calcularResumo() {
            List<Gasto> gastos = gastoRepository.findAll();
            List<Salario> salarios = salarioRepository.findAll();
            Double totalGastos = gastos.stream().mapToDouble(Gasto::getValor).sum();
            Double totalSalario= salarios.stream().mapToDouble(Salario::getValor).sum();
            Double saldo = totalSalario - totalGastos;

             ResumoMensal resumo = new ResumoMensal();
             resumo.setTotalGasto(totalGastos);
             resumo.setTotalSalario(totalSalario);
             resumo.setSaldo(saldo);

             if (saldo > 0) {
                 resumo.setMensagem("Parabéns! Você economizou esse mês!\uD83C\uDF89");
             }
             if (saldo < 0) {
                 resumo.setMensagem("Atenção! Seus gastos ultrapassaram o salário esse mês!");
             }
             return resumo;
        }


}
