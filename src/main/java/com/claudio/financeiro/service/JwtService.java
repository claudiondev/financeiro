package com.claudio.financeiro.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key chave;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret deve ter no mínimo 32 caracteres (256 bits)");
        }
        this.chave = Keys.hmacShaKeyFor(keyBytes);
        log.info("JwtService inicializado com segredo de {} bytes", keyBytes.length);
    }

    public String gerarToken(String email) {
        Date agora = new Date();
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(agora)
                .setExpiration(new Date(agora.getTime() + expiration))
                .signWith(chave, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extrairEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(chave)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validarToken(String token, String email) {
        try {
            return extrairEmail(token).equals(email);
        } catch (ExpiredJwtException e) {
            log.debug("Token expirado para o usuário: {}", email);
            return false;
        } catch (JwtException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }
}
