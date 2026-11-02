import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Regras do sistema: dashboard, transações, despesas fixas e anotações. */
final class Finance {
    private Finance() {}

    // "Hoje" e "mês atual" sempre no horário de Brasília (o servidor roda em UTC).
    private static final String TODAY = "(now() AT TIME ZONE 'America/Sao_Paulo')::date";

    // Colunas de uma transação, já com os nomes que o front espera.
    private static final String TX_COLUMNS =
            "id, type, description, amount_cents AS \"amountCents\", category, date::text AS date, "
            + "fixed_expense_id AS \"fixedExpenseId\"";

    private static Object ok() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        return m;
    }

    // ================= despesas fixas =================

    /**
     * Cada despesa fixa vira uma transação por mês, do mês do primeiro vencimento até o mês atual.
     * Roda antes de listar/somar. Se a ocorrência do mês já existe, não faz nada (UNIQUE no banco).
     */
    private static void generateOccurrences(UUID userId) {
        Db.update("""
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

    private static final String FIXED_COLUMNS =
            "id, description, amount_cents AS \"amountCents\", category, due_date::text AS \"dueDate\"";

    static Object listFixed(UUID userId) {
        return Db.query("SELECT " + FIXED_COLUMNS + " FROM fixed_expenses WHERE user_id = ? ORDER BY created_at", userId);
    }

    static Object createFixed(UUID userId, Map<String, Object> b) {
        return Db.query("INSERT INTO fixed_expenses (user_id, description, amount_cents, category, due_date) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING " + FIXED_COLUMNS,
                userId,
                Validate.text(b, "description", "Preencha todos os campos.", 200),
                Validate.cents(b),
                Validate.text(b, "category", "Preencha todos os campos.", 100),
                Validate.date(b, "dueDate", true)).get(0);
    }

    static Object updateFixed(UUID userId, String id, Map<String, Object> b) {
        List<Map<String, Object>> rows = Db.query("UPDATE fixed_expenses SET description = ?, amount_cents = ?, "
                        + "category = ?, due_date = ? WHERE id = ? AND user_id = ? RETURNING " + FIXED_COLUMNS,
                Validate.text(b, "description", "Preencha todos os campos.", 200),
                Validate.cents(b),
                Validate.text(b, "category", "Preencha todos os campos.", 100),
                Validate.date(b, "dueDate", true),
                Validate.id(id), userId);
        if (rows.isEmpty()) throw new ApiException(404, "Registro não encontrado.");
        return rows.get(0);
    }

    /** Apaga a despesa fixa e (ON DELETE CASCADE) todas as ocorrências dela no histórico. */
    static Object deleteFixed(UUID userId, String id) {
        if (Db.update("DELETE FROM fixed_expenses WHERE id = ? AND user_id = ?", Validate.id(id), userId) == 0) {
            throw new ApiException(404, "Registro não encontrado.");
        }
        return ok();
    }

    // ================= transações (rendas e despesas avulsas) =================

    /** GET /api/transactions?type=&month=&year=&limit= — mais recentes primeiro. */
    static Object listTransactions(UUID userId, Map<String, String> q) {
        generateOccurrences(userId);
        StringBuilder sql = new StringBuilder("SELECT " + TX_COLUMNS + " FROM transactions WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        String type = q.get("type");
        if (type != null) {
            if (!List.of("income", "variable", "fixed").contains(type)) throw new ApiException(400, "Tipo inválido.");
            sql.append(" AND type = ?");
            params.add(type);
        }
        try {
            if (q.get("month") != null) {
                sql.append(" AND EXTRACT(MONTH FROM date) = ?");
                params.add(Integer.parseInt(q.get("month")));
            }
            if (q.get("year") != null) {
                sql.append(" AND EXTRACT(YEAR FROM date) = ?");
                params.add(Integer.parseInt(q.get("year")));
            }
            sql.append(" ORDER BY date DESC, created_at DESC");
            if (q.get("limit") != null) {
                sql.append(" LIMIT ?");
                params.add(Math.min(Integer.parseInt(q.get("limit")), 500));
            }
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Filtro inválido.");
        }
        return Db.query(sql.toString(), params.toArray());
    }

    private static String transactionType(Map<String, Object> b) {
        Object t = b.get("type");
        if (!"income".equals(t) && !"variable".equals(t)) throw new ApiException(400, "Tipo inválido.");
        return (String) t;
    }

    static Object createTransaction(UUID userId, Map<String, Object> b) {
        String type = transactionType(b);
        String category = "income".equals(type) ? "" : Validate.optionalText(b, "category", 100);
        return Db.query("INSERT INTO transactions (user_id, type, description, amount_cents, category, date) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING " + TX_COLUMNS,
                userId, type,
                Validate.text(b, "description", "Preencha a descrição e a data.", 200),
                Validate.cents(b), category, Validate.date(b, "date", true)).get(0);
    }

    static Object updateTransaction(UUID userId, String id, Map<String, Object> b) {
        String type = transactionType(b);
        String category = "income".equals(type) ? "" : Validate.optionalText(b, "category", 100);
        List<Map<String, Object>> rows = Db.query("UPDATE transactions SET type = ?, description = ?, amount_cents = ?, "
                        + "category = ?, date = ? WHERE id = ? AND user_id = ? AND type <> 'fixed' RETURNING " + TX_COLUMNS,
                type,
                Validate.text(b, "description", "Preencha a descrição e a data.", 200),
                Validate.cents(b), category, Validate.date(b, "date", true),
                Validate.id(id), userId);
        if (rows.isEmpty()) throw new ApiException(404, "Registro não encontrado.");
        return rows.get(0);
    }

    static Object deleteTransaction(UUID userId, String id) {
        // As ocorrências de despesa fixa ('fixed') só saem apagando a despesa fixa.
        if (Db.update("DELETE FROM transactions WHERE id = ? AND user_id = ? AND type <> 'fixed'",
                Validate.id(id), userId) == 0) {
            throw new ApiException(404, "Registro não encontrado.");
        }
        return ok();
    }

    // ================= dashboard =================

    /** Totais do mês atual + saldo geral + últimas movimentações, numa chamada só. */
    static Object dashboard(UUID userId) {
        generateOccurrences(userId);
        Map<String, Object> out = new LinkedHashMap<>(Db.query("""
                SELECT COALESCE(SUM(CASE WHEN type = 'income' THEN amount_cents ELSE -amount_cents END), 0) AS balance,
                       COALESCE(SUM(amount_cents) FILTER (WHERE type = 'income'   AND date >= m.start AND date < m.start + interval '1 month'), 0) AS income,
                       COALESCE(SUM(amount_cents) FILTER (WHERE type = 'fixed'    AND date >= m.start AND date < m.start + interval '1 month'), 0) AS fixed,
                       COALESCE(SUM(amount_cents) FILTER (WHERE type = 'variable' AND date >= m.start AND date < m.start + interval '1 month'), 0) AS variable
                FROM transactions,
                     (SELECT date_trunc('month', %s::timestamp)::date AS start) m
                WHERE user_id = ?
                """.formatted(TODAY), userId).get(0));
        out.put("recent", Db.query("SELECT " + TX_COLUMNS
                + " FROM transactions WHERE user_id = ? ORDER BY date DESC, created_at DESC LIMIT 8", userId));
        return out;
    }

    // ================= anotações =================

    private static final String NOTE_COLUMNS = "id, title, content, note_date::text AS date";

    static Object listNotes(UUID userId) {
        return Db.query("SELECT " + NOTE_COLUMNS + " FROM notes WHERE user_id = ? ORDER BY updated_at DESC", userId);
    }

    static Object createNote(UUID userId, Map<String, Object> b) {
        LocalDate date = Validate.date(b, "date", false);
        return Db.query("INSERT INTO notes (user_id, title, content, note_date) VALUES (?, ?, ?, ?) RETURNING " + NOTE_COLUMNS,
                userId,
                Validate.text(b, "title", "Informe o título e o conteúdo da anotação.", 200),
                Validate.text(b, "content", "Informe o título e o conteúdo da anotação.", 5000),
                date).get(0);
    }

    static Object updateNote(UUID userId, String id, Map<String, Object> b) {
        LocalDate date = Validate.date(b, "date", false);
        List<Map<String, Object>> rows = Db.query("UPDATE notes SET title = ?, content = ?, note_date = ?, updated_at = now() "
                        + "WHERE id = ? AND user_id = ? RETURNING " + NOTE_COLUMNS,
                Validate.text(b, "title", "Informe o título e o conteúdo da anotação.", 200),
                Validate.text(b, "content", "Informe o título e o conteúdo da anotação.", 5000),
                date, Validate.id(id), userId);
        if (rows.isEmpty()) throw new ApiException(404, "Registro não encontrado.");
        return rows.get(0);
    }

    static Object deleteNote(UUID userId, String id) {
        if (Db.update("DELETE FROM notes WHERE id = ? AND user_id = ?", Validate.id(id), userId) == 0) {
            throw new ApiException(404, "Registro não encontrado.");
        }
        return ok();
    }
}
