package com.meufinanceiro.service;

import com.meufinanceiro.model.DashboardSummary;
import com.meufinanceiro.model.Totals;
import com.meufinanceiro.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DashboardService {
    private final TransactionRepository repository;

    public DashboardService(TransactionRepository repository) {
        this.repository = repository;
    }

    /** Totais do mês + saldo geral + últimas 8 movimentações. */
    public DashboardSummary summary(UUID userId) {
        repository.generateFixedOccurrences(userId);
        Totals t = repository.totals(userId);
        return new DashboardSummary(t.balance(), t.income(), t.fixed(), t.variable(),
                repository.find(userId, null, null, null, 8));
    }
}
