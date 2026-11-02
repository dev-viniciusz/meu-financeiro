package com.meufinanceiro.controller;

import com.meufinanceiro.dto.Requests;
import com.meufinanceiro.model.SessionResponse;
import com.meufinanceiro.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas públicas: cadastro e login. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService service;

    public AuthController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public SessionResponse register(@RequestBody Requests.Register body) {
        return service.register(body);
    }

    @PostMapping("/login")
    public SessionResponse login(@RequestBody Requests.Login body) {
        return service.login(body);
    }
}
