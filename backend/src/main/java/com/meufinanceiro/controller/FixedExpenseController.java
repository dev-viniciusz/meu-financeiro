package com.meufinanceiro.controller;

import com.meufinanceiro.dto.Requests.FixedExpenseBody;
import com.meufinanceiro.model.FixedExpense;
import com.meufinanceiro.service.FixedExpenseService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fixed-expenses")
public class FixedExpenseController {
    private final FixedExpenseService service;

    public FixedExpenseController(FixedExpenseService service) {
        this.service = service;
    }

    @GetMapping
    public List<FixedExpense> list(@RequestAttribute("userId") UUID userId) {
        return service.list(userId);
    }

    @PostMapping
    public FixedExpense create(@RequestAttribute("userId") UUID userId, @RequestBody FixedExpenseBody body) {
        return service.create(userId, body);
    }

    @PutMapping("/{id}")
    public FixedExpense update(@RequestAttribute("userId") UUID userId, @PathVariable("id") String id,
                               @RequestBody FixedExpenseBody body) {
        return service.update(userId, id, body);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@RequestAttribute("userId") UUID userId, @PathVariable("id") String id) {
        service.delete(userId, id);
        return Map.of("ok", true);
    }
}
