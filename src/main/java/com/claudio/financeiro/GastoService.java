package com.claudio.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GastoService {

    @Autowired
    private GastoRepository gastoRepository;
    public Gasto salvar  (Gasto gasto) { return gastoRepository.save(gasto);
    }

}
