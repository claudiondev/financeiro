package com.claudio.financeiro.config;

import com.claudio.financeiro.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Bloqueia qualquer escrita vinda da conta demo (ver DemoDataSeeder), pra que os dados de
 * exemplo nunca fiquem sujos por quem estiver só avaliando o app. Roda depois do JwtFilter
 * (interceptors executam após os filtros de servlet), então o usuário autenticado já está
 * no SecurityContext quando este código roda.
 */
@Component
public class DemoReadOnlyInterceptor implements HandlerInterceptor {

    private static final Set<String> METODOS_DE_ESCRITA = Set.of(
            HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.PATCH.name(), HttpMethod.DELETE.name()
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!METODOS_DE_ESCRITA.contains(request.getMethod())) {
            return true;
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario usuario && usuario.isDemo()) {
            // 423 Locked, não 403: o interceptor de resposta do frontend trata todo 403 (e 401)
            // como sessão inválida e força logout — usar 403 aqui deslogaria o visitante no
            // meio da demo antes dele ver o aviso. 423 não colide com essa regra.
            response.setStatus(423);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"erro\": \"Ação desabilitada no modo demo\"}");
            return false;
        }

        return true;
    }
}
