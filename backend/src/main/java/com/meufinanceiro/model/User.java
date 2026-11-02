package com.meufinanceiro.model;

public record User(String id, String name, String email, String passwordHash) {
}
