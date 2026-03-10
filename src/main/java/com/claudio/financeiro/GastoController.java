package com.claudio.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gastos")
public class GastoController {
    @Autowired
    private GastoService gastoService;

    @PostMapping
    public Gasto criar (@RequestBody Gasto gasto) {return gastoService.salvar(gasto);
    }
}
