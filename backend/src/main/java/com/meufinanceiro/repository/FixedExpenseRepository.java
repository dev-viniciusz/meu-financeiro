package com.meufinanceiro.repository;

import com.meufinanceiro.model.FixedExpense;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FixedExpenseRepository {
    private static final String COLUMNS =
            "id::text AS id, description, amount_cents AS \"amountCents\", category, due_date::text AS \"dueDate\"";

    private static final RowMapper<FixedExpense> MAPPER = (rs, i) -> new FixedExpense(
            rs.getString("id"), rs.getString("description"), rs.getLong("amountCents"),
            rs.getString("category"), rs.getString("dueDate"));

    private final JdbcTemplate jdbc;

    public FixedExpenseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<FixedExpense> findAll(UUID userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM fixed_expenses WHERE user_id = ? ORDER BY created_at",
                MAPPER, userId);
    }

    public FixedExpense create(UUID userId, String description, long amountCents, String category, String dueDate) {
        return jdbc.queryForObject("INSERT INTO fixed_expenses (user_id, description, amount_cents, category, due_date) "
                        + "VALUES (?, ?, ?, ?, ?::date) RETURNING " + COLUMNS,
                MAPPER, userId, description, amountCents, category, dueDate);
    }

    public Optional<FixedExpense> update(UUID userId, UUID id, String description, long amountCents,
                                         String category, String dueDate) {
        return jdbc.query("UPDATE fixed_expenses SET description = ?, amount_cents = ?, category = ?, "
                                + "due_date = ?::date WHERE id = ? AND user_id = ? RETURNING " + COLUMNS,
                        MAPPER, description, amountCents, category, dueDate, id, userId)
                .stream().findFirst();
    }

    /** Apaga também as ocorrências no histórico (ON DELETE CASCADE no banco). */
    public boolean delete(UUID userId, UUID id) {
        return jdbc.update("DELETE FROM fixed_expenses WHERE id = ? AND user_id = ?", id, userId) > 0;
    }
}
