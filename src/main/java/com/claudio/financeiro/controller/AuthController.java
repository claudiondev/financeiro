package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.EmailRequest;
import com.claudio.financeiro.dto.LoginRequest;
import com.claudio.financeiro.dto.RedefinirSenhaRequest;
import com.claudio.financeiro.dto.RegistrarRequest;
import com.claudio.financeiro.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registrar")
    public ResponseEntity<String> registrar(@Valid @RequestBody RegistrarRequest request) {
        authService.registrar(request.getEmail(), request.getSenha(), request.getNome());
        return ResponseEntity.ok("Usuario registrado com sucesso!");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getSenha());
        return ResponseEntity.ok(token);
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<String> recuperarSenha(@Valid @RequestBody EmailRequest request) {
        authService.recuperarSenha(request.getEmail());
        return ResponseEntity.ok("Se o e-mail estiver cadastrado, você receberá as instruções em breve.");
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<String> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        authService.redefinirSenha(request.getCodigo(), request.getNovaSenha());
        return ResponseEntity.ok("Senha redefinida com sucesso!");
    }
}
