package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.EmailRequest;
import com.claudio.financeiro.dto.RedefinirSenhaRequest;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.UsuarioRepository;
import com.claudio.financeiro.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/registrar")
    public ResponseEntity<String> registrar(@RequestBody Usuario usuario) {
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuarioRepository.save(usuario);
        return ResponseEntity.ok("Usuario registrado com sucesso!");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Usuario usuario) {
        Usuario encontrado = usuarioRepository.findByEmail(usuario.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));

        if (!passwordEncoder.matches(usuario.getSenha(), encontrado.getSenha())) {
            return ResponseEntity.status(401).body("Senha incorreta");
        }

        String token = jwtService.gerarToken(encontrado.getEmail());
        return ResponseEntity.ok(token);
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<String> recuperarSenha(@RequestBody EmailRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email nao encontrado"));

        String codigo = String.valueOf((int)(Math.random() * 900000) + 100000);
        usuario.setCodigoRecuperacao(codigo);
        usuarioRepository.save(usuario);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getEmail());
        message.setSubject("Recuperacao de senha");
        message.setText("Seu codigo de recuperacao e: " + codigo);
        mailSender.send(message);

        return ResponseEntity.ok("Codigo enviado para o email!");
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<String> redefinirSenha(@RequestBody RedefinirSenhaRequest request) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        for (Usuario usuario : usuarios) {
            if (request.getCodigo().equals(usuario.getCodigoRecuperacao())) {
                usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
                usuario.setCodigoRecuperacao(null); // Limpa o código após usar
                usuarioRepository.save(usuario);
                return ResponseEntity.ok("Senha redefinida com sucesso!");
            }
        }
        return ResponseEntity.status(400).body("Codigo invalido!");
    }
}