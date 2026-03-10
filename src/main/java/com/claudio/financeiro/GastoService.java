package com.claudio.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Service
public class GastoService {

    @Autowired
    private GastoRepository gastoRepository;
    public Gasto salvar  (Gasto gasto) { return gastoRepository.save(gasto);
    }
    public List<Gasto> listar() { return gastoRepository.findAll();
    }

}
