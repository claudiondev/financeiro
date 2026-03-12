package com.claudio.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/salario")
public class SalarioController {
    @Autowired
    private SalarioService salarioService;

    @PostMapping
    public Salario criar (@RequestBody Salario salario) {return salarioService.salvar(salario);
    }
    @GetMapping
    public List <Salario> listar() { return salarioService.listar();
    }
    @DeleteMapping("/{id}")
    public void deletar (@PathVariable Long id ) {salarioService.deletar(id);
    }
}
