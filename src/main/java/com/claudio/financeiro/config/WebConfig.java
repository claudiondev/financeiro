package com.claudio.financeiro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final DemoReadOnlyInterceptor demoReadOnlyInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor, DemoReadOnlyInterceptor demoReadOnlyInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.demoReadOnlyInterceptor = demoReadOnlyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Só os endpoints alvo de força bruta/flooding precisam de rate limiting.
        // /auth/registrar já tem proteção via e-mail único (409) e /auth/redefinir-senha
        // exige um código válido — sem ele, uma tentativa em loop não serve de nada.
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/auth/login", "/auth/recuperar-senha");

        // /auth/** fica de fora: é onde o próprio /auth/demo vive, e nenhum outro endpoint
        // de auth muda dado do usuário demo (login/registro/recuperação são de outra conta).
        registry.addInterceptor(demoReadOnlyInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**");
    }
}
