package com.meufinanceiro.model;

/** type: "income" (renda), "variable" (despesa avulsa) ou "fixed" (ocorrência mensal de despesa fixa). */
public record Transaction(String id, String type, String description, long amountCents,
                          String category, String date, String fixedExpenseId) {
}
