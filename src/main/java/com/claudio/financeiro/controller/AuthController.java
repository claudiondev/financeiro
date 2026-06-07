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

import java.security.SecureRandom;
import java.time.LocalDateTime;

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

    private static final SecureRandom secureRandom = new SecureRandom();

    @PostMapping("/registrar")
    public ResponseEntity<String> registrar(@RequestBody Usuario usuario) {
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuarioRepository.save(usuario);
        return ResponseEntity.ok("Usuario registrado com sucesso!");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Usuario usuario) {
        // Mensagem genérica: não revela se o e-mail existe ou se a senha está errada
        Usuario encontrado = usuarioRepository.findByEmail(usuario.getEmail())
                .orElse(null);

        if (encontrado == null || !passwordEncoder.matches(usuario.getSenha(), encontrado.getSenha())) {
            return ResponseEntity.status(401).body("Credenciais inválidas");
        }

        String token = jwtService.gerarToken(encontrado.getEmail());
        return ResponseEntity.ok(token);
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<String> recuperarSenha(@RequestBody EmailRequest request) {
        usuarioRepository.findByEmail(request.getEmail()).ifPresent(usuario -> {
            // SecureRandom em vez de Math.random() — criptograficamente seguro
            String codigo = String.valueOf(secureRandom.nextInt(900000) + 100000);
            usuario.setCodigoRecuperacao(codigo);
            // Código válido por 15 minutos
            usuario.setCodigoRecuperacaoExpiracao(LocalDateTime.now().plusMinutes(15));
            usuarioRepository.save(usuario);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.getEmail());
            message.setSubject("Recuperação de senha");
            message.setText("Seu código de recuperação é: " + codigo + "\nEle expira em 15 minutos.");
            mailSender.send(message);
        });

        // Resposta genérica — não revela se o e-mail está cadastrado
        return ResponseEntity.ok("Se o e-mail estiver cadastrado, você receberá as instruções em breve.");
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<String> redefinirSenha(@RequestBody RedefinirSenhaRequest request) {
        Usuario usuario = usuarioRepository.findByCodigoRecuperacao(request.getCodigo())
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(400).body("Código inválido.");
        }

        if (usuario.getCodigoRecuperacaoExpiracao() == null
                || LocalDateTime.now().isAfter(usuario.getCodigoRecuperacaoExpiracao())) {
            return ResponseEntity.status(400).body("Código expirado. Solicite um novo.");
        }

        usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        usuario.setCodigoRecuperacao(null);
        usuario.setCodigoRecuperacaoExpiracao(null);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok("Senha redefinida com sucesso!");
    }
}
