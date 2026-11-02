package com.meufinanceiro.repository;

import com.meufinanceiro.model.Totals;
import com.meufinanceiro.model.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRepository {
    // "Hoje" sempre no horário de Brasília (o servidor roda em UTC).
    private static final String TODAY = "(now() AT TIME ZONE 'America/Sao_Paulo')::date";

    private static final String COLUMNS =
            "id::text AS id, type, description, amount_cents AS \"amountCents\", category, date::text AS date, "
            + "fixed_expense_id::text AS \"fixedExpenseId\"";

    private static final RowMapper<Transaction> MAPPER = (rs, i) -> new Transaction(
            rs.getString("id"), rs.getString("type"), rs.getString("description"), rs.getLong("amountCents"),
            rs.getString("category"), rs.getString("date"), rs.getString("fixedExpenseId"));

    private final JdbcTemplate jdbc;

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Cada despesa fixa vira uma transação por mês, do mês do primeiro vencimento até o mês atual.
     * Se a ocorrência do mês já existe, não faz nada (UNIQUE no banco).
     */
    public void generateFixedOccurrences(UUID userId) {
        jdbc.update("""
                INSERT INTO transactions
                    (user_id, type, description, amount_cents, category, date, fixed_expense_id, occurrence_month)
                SELECT f.user_id, 'fixed', f.description, f.amount_cents, f.category,
                       m::date + (LEAST(EXTRACT(DAY FROM f.due_date)::int,
                                        EXTRACT(DAY FROM (m + interval '1 month - 1 day'))::int) - 1),
                       f.id, m::date
                FROM fixed_expenses f,
                     LATERAL generate_series(date_trunc('month', f.due_date::timestamp),
                                             date_trunc('month', %s::timestamp),
                                             interval '1 month') AS m
                WHERE f.user_id = ?
                ON CONFLICT (fixed_expense_id, occurrence_month) DO NOTHING
                """.formatted(TODAY), userId);
    }

    /** Filtros opcionais (null = sem filtro). Mais recentes primeiro. */
    public List<Transaction> find(UUID userId, String type, Integer month, Integer year, Integer limit) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM transactions WHERE user_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (type != null) {
            sql.append(" AND type = ?");
            args.add(type);
        }
        if (month != null) {
            sql.append(" AND EXTRACT(MONTH FROM date) = ?");
            args.add(month);
        }
        if (year != null) {
            sql.append(" AND EXTRACT(YEAR FROM date) = ?");
            args.add(year);
        }
        sql.append(" ORDER BY date DESC, created_at DESC");
        if (limit != null) {
            sql.append(" LIMIT ?");
            args.add(limit);
        }
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public Transaction create(UUID userId, String type, String description, long amountCents,
                              String category, String date) {
        return jdbc.queryForObject("INSERT INTO transactions (user_id, type, description, amount_cents, category, date) "
                        + "VALUES (?, ?, ?, ?, ?, ?::date) RETURNING " + COLUMNS,
                MAPPER, userId, type, description, amountCents, category, date);
    }

    /** Só rendas e despesas avulsas; as ocorrências 'fixed' não são editáveis aqui. */
    public Optional<Transaction> update(UUID userId, UUID id, String type, String description,
                                        long amountCents, String category, String date) {
        return jdbc.query("UPDATE transactions SET type = ?, description = ?, amount_cents = ?, category = ?, "
                                + "date = ?::date WHERE id = ? AND user_id = ? AND type <> 'fixed' RETURNING " + COLUMNS,
                        MAPPER, type, description, amountCents, category, date, id, userId)
                .stream().findFirst();
    }

    public boolean delete(UUID userId, UUID id) {
        return jdbc.update("DELETE FROM transactions WHERE id = ? AND user_id = ? AND type <> 'fixed'",
                id, userId) > 0;
    }

    /** Saldo geral + totais do mês atual. */
    public Totals totals(UUID userId) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN type = 'income' THEN amount_cents ELSE -amount_cents END), 0) AS balance,
                       COALESCE(SUM(amount_cents) FILTER (WHERE type = 'income'   AND date >= m.start AND date < m.start + interval '1 month'), 0) AS income,
                       COALESCE(SUM(amount_cents) FILTER (WHERE type = 'fixed'    AND date >= m.start AND date < m.start + interval '1 month'), 0) AS fixed,
                       COALESCE(SUM(amount_cents) FILTER (WHERE type = 'variable' AND date >= m.start AND date < m.start + interval '1 month'), 0) AS variable
                FROM transactions,
                     (SELECT date_trunc('month', %s::timestamp)::date AS start) m
                WHERE user_id = ?
                """.formatted(TODAY),
                (rs, i) -> new Totals(rs.getLong("balance"), rs.getLong("income"),
                        rs.getLong("fixed"), rs.getLong("variable")),
                userId);
    }
}
