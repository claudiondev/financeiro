package com.claudio.financeiro.service;

import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.repository.SalarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SalarioService {

    @Autowired
    private SalarioRepository salarioRepository;

    public Salario salvar(Salario salario) {
        return salarioRepository.save(salario);
    }

    public List<Salario> listar() {
        return salarioRepository.findAll();
    }

    public List<Salario> listarPorUsuario(Long usuarioId) {
        return salarioRepository.findByUsuarioId(usuarioId);
    }

    public void deletar(Long id) {
        salarioRepository.deleteById(id);
    }
}