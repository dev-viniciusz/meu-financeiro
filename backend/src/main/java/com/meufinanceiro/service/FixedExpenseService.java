package com.meufinanceiro.service;

import com.meufinanceiro.dto.Requests.FixedExpenseBody;
import com.meufinanceiro.model.FixedExpense;
import com.meufinanceiro.repository.FixedExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FixedExpenseService {
    private static final String REQUIRED = "Preencha todos os campos.";
    private final FixedExpenseRepository repository;

    public FixedExpenseService(FixedExpenseRepository repository) {
        this.repository = repository;
    }

    public List<FixedExpense> list(UUID userId) {
        return repository.findAll(userId);
    }

    public FixedExpense create(UUID userId, FixedExpenseBody b) {
        return repository.create(userId, Validate.text(b.description(), REQUIRED, 200), Validate.cents(b.amountCents()),
                Validate.text(b.category(), REQUIRED, 100), Validate.date(b.dueDate(), true));
    }

    public FixedExpense update(UUID userId, String id, FixedExpenseBody b) {
        return repository.update(userId, Validate.id(id), Validate.text(b.description(), REQUIRED, 200),
                        Validate.cents(b.amountCents()), Validate.text(b.category(), REQUIRED, 100),
                        Validate.date(b.dueDate(), true))
                .orElseThrow(Validate::notFound);
    }

    public void delete(UUID userId, String id) {
        if (!repository.delete(userId, Validate.id(id))) throw Validate.notFound();
    }
}
