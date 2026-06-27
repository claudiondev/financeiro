package com.claudio.financeiro.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração do MVC: registra interceptors HTTP na cadeia de execução.
 *
 * Por que WebMvcConfigurer e não WebMvcConfigurationSupport?
 * WebMvcConfigurer é a interface de extensão que PRESERVA toda a auto-
 * configuração do Spring Boot. Já WebMvcConfigurationSupport a SUBSTITUI,
 * desabilitando features automáticas como serialização JSON e tratamento de
 * recursos estáticos. Nunca use WebMvcConfigurationSupport a menos que
 * queira assumir o controle total do MVC.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /*
         * Aplica rate limiting apenas nos endpoints de autenticação que
         * são alvo de ataques de força bruta e flooding:
         *   - /auth/login: tentativas de senha
         *   - /auth/recuperar-senha: flooding de e-mails de recuperação
         *
         * O /auth/registrar e /auth/redefinir-senha não entram aqui porque:
         *   - Registrar: já tem proteção de e-mail único (409 Conflict)
         *   - Redefinir: requer um código válido — sem o código, é inútil
         */
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/auth/login", "/auth/recuperar-senha");
    }
}
