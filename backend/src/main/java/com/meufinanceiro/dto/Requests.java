package com.meufinanceiro.dto;

/** Corpos de requisição (JSON) que o front envia. */
public final class Requests {
    private Requests() {
    }

    public record Register(String name, String email, String password) {
    }

    public record Login(String email, String password) {
    }

    public record ProfileUpdate(String name, String email) {
    }

    public record TransactionBody(String type, String description, Long amountCents, String category, String date) {
    }

    public record FixedExpenseBody(String description, Long amountCents, String category, String dueDate) {
    }

    public record NoteBody(String title, String content, String date) {
    }
}
