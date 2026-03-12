package com.claudio.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gastos")
public class GastoController {
    @Autowired
    private GastoService gastoService;

    @PostMapping
    public Gasto criar (@RequestBody Gasto gasto) {return gastoService.salvar(gasto);
    }
    @GetMapping
    public List <Gasto> listar() { return gastoService.listar();
    }
    @DeleteMapping("/{id}")
    public void deletar (@PathVariable Long id ) {gastoService.deletar(id);
    }
    @GetMapping("/resumo")
    public ResumoMensal resumo() {return gastoService.calcularResumo();
    }
}
