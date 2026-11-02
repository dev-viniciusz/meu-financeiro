package com.meufinanceiro.config;

import com.meufinanceiro.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Roda antes dos controllers: confere o token de login e guarda o id do usuário na requisição. */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final UserService userService;

    public AuthInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equals(request.getMethod())) return true; // pré-checagem do CORS
        request.setAttribute("userId", userService.userIdFromHeader(request.getHeader("Authorization")));
        return true;
    }
}
