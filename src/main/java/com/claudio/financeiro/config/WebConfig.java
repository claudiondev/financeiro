package com.claudio.financeiro.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Só os endpoints alvo de força bruta/flooding precisam de rate limiting.
        // /auth/registrar já tem proteção via e-mail único (409) e /auth/redefinir-senha
        // exige um código válido — sem ele, uma tentativa em loop não serve de nada.
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/auth/login", "/auth/recuperar-senha");
    }
}
