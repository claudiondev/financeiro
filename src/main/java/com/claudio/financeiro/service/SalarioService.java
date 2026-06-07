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

    public void deletar(Long id, Long usuarioId) {
        Salario salario = salarioRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Salário não encontrado"));
        if (!salario.getUsuario().getId().equals(usuarioId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Acesso negado");
        }
        salarioRepository.deleteById(id);
    }
}