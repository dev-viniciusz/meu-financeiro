package com.meufinanceiro.model;

public record FixedExpense(String id, String description, long amountCents, String category, String dueDate) {
}
