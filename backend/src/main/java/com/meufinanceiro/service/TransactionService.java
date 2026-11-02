package com.meufinanceiro.service;

import com.meufinanceiro.dto.Requests.TransactionBody;
import com.meufinanceiro.exception.ApiException;
import com.meufinanceiro.model.Transaction;
import com.meufinanceiro.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Rendas, despesas avulsas e histórico (que inclui as despesas fixas do mês). */
@Service
public class TransactionService {
    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public List<Transaction> list(UUID userId, String type, Integer month, Integer year, Integer limit) {
        if (type != null && !List.of("income", "variable", "fixed").contains(type)) {
            throw new ApiException(400, "Tipo inválido.");
        }
        repository.generateFixedOccurrences(userId); // garante as despesas fixas do mês antes de listar
        return repository.find(userId, type, month, year, limit == null ? null : Math.min(limit, 500));
    }

    public Transaction create(UUID userId, TransactionBody b) {
        String type = type(b);
        return repository.create(userId, type,
                Validate.text(b.description(), "Preencha a descrição e a data.", 200),
                Validate.cents(b.amountCents()), category(type, b), Validate.date(b.date(), true));
    }

    public Transaction update(UUID userId, String id, TransactionBody b) {
        String type = type(b);
        return repository.update(userId, Validate.id(id), type,
                        Validate.text(b.description(), "Preencha a descrição e a data.", 200),
                        Validate.cents(b.amountCents()), category(type, b), Validate.date(b.date(), true))
                .orElseThrow(Validate::notFound);
    }

    public void delete(UUID userId, String id) {
        if (!repository.delete(userId, Validate.id(id))) throw Validate.notFound();
    }

    // Só "income" e "variable" podem ser criadas/editadas por aqui.
    private String type(TransactionBody b) {
        if (!"income".equals(b.type()) && !"variable".equals(b.type())) throw new ApiException(400, "Tipo inválido.");
        return b.type();
    }

    private String category(String type, TransactionBody b) {
        return "income".equals(type) ? "" : Validate.optionalText(b.category(), 100);
    }
}
