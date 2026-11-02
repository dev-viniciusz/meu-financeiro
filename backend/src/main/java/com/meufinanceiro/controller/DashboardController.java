package com.meufinanceiro.controller;

import com.meufinanceiro.model.DashboardSummary;
import com.meufinanceiro.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardSummary get(@RequestAttribute("userId") UUID userId) {
        return service.summary(userId);
    }
}
