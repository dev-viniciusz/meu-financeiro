package com.meufinanceiro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Usado pela hospedagem para saber se o servidor está de pé. */
@RestController
public class HealthController {
    @GetMapping({"/", "/api/health"})
    public Map<String, Boolean> health() {
        return Map.of("ok", true);
    }
}
