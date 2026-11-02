package com.meufinanceiro.model;

import java.util.List;

public record DashboardSummary(long balance, long income, long fixed, long variable, List<Transaction> recent) {
}
