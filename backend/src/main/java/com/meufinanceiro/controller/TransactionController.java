package com.meufinanceiro.controller;

import com.meufinanceiro.dto.Requests.TransactionBody;
import com.meufinanceiro.model.Transaction;
import com.meufinanceiro.service.TransactionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Transaction> list(@RequestAttribute("userId") UUID userId,
                                  @RequestParam(name = "type", required = false) String type,
                                  @RequestParam(name = "month", required = false) Integer month,
                                  @RequestParam(name = "year", required = false) Integer year,
                                  @RequestParam(name = "limit", required = false) Integer limit) {
        return service.list(userId, type, month, year, limit);
    }

    @PostMapping
    public Transaction create(@RequestAttribute("userId") UUID userId, @RequestBody TransactionBody body) {
        return service.create(userId, body);
    }

    @PutMapping("/{id}")
    public Transaction update(@RequestAttribute("userId") UUID userId, @PathVariable("id") String id,
                              @RequestBody TransactionBody body) {
        return service.update(userId, id, body);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@RequestAttribute("userId") UUID userId, @PathVariable("id") String id) {
        service.delete(userId, id);
        return Map.of("ok", true);
    }
}
