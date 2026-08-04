package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.ConfirmarImportacaoRequest;
import com.claudio.financeiro.dto.ImportacaoResultadoDTO;
import com.claudio.financeiro.dto.TransacaoImportadaDTO;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.ImportacaoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/importacao")
public class ImportacaoController {

    private final ImportacaoService importacaoService;

    public ImportacaoController(ImportacaoService importacaoService) {
        this.importacaoService = importacaoService;
    }

    // Só lê e devolve — nada é salvo até /confirmar. Usuário revisa/ajusta categoria no frontend antes.
    @PostMapping("/ofx")
    public List<TransacaoImportadaDTO> processarOfx(@RequestParam("arquivo") MultipartFile arquivo, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return importacaoService.processarArquivo(arquivo, usuarioLogado.getId());
    }

    @PostMapping("/confirmar")
    public ImportacaoResultadoDTO confirmar(@Valid @RequestBody ConfirmarImportacaoRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return importacaoService.confirmar(request.getItens(), usuarioLogado);
    }
}
