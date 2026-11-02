package com.meufinanceiro.model;

/** Saldo geral e totais do mês atual, em centavos. */
public record Totals(long balance, long income, long fixed, long variable) {
}
