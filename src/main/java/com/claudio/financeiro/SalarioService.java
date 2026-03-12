package com.claudio.financeiro;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalarioService {

    @Autowired
    private SalarioRepository salarioRepository;
    public Salario salvar  (Salario salario) { return salarioRepository.save(salario);
    }
    public List<Salario> listar() { return salarioRepository.findAll();
    }

    public void deletar(Long id) {
        salarioRepository.deleteById(id);
    }
}
