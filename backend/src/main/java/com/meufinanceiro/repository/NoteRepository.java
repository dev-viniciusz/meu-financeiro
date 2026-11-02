package com.meufinanceiro.repository;

import com.meufinanceiro.model.Note;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NoteRepository {
    private static final String COLUMNS = "id::text AS id, title, content, note_date::text AS date";

    private static final RowMapper<Note> MAPPER = (rs, i) -> new Note(
            rs.getString("id"), rs.getString("title"), rs.getString("content"), rs.getString("date"));

    private final JdbcTemplate jdbc;

    public NoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Note> findAll(UUID userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM notes WHERE user_id = ? ORDER BY updated_at DESC",
                MAPPER, userId);
    }

    /** date pode ser null (anotação sem lembrete). */
    public Note create(UUID userId, String title, String content, String date) {
        return jdbc.queryForObject("INSERT INTO notes (user_id, title, content, note_date) "
                + "VALUES (?, ?, ?, ?::date) RETURNING " + COLUMNS, MAPPER, userId, title, content, date);
    }

    public Optional<Note> update(UUID userId, UUID id, String title, String content, String date) {
        return jdbc.query("UPDATE notes SET title = ?, content = ?, note_date = ?::date, updated_at = now() "
                                + "WHERE id = ? AND user_id = ? RETURNING " + COLUMNS,
                        MAPPER, title, content, date, id, userId)
                .stream().findFirst();
    }

    public boolean delete(UUID userId, UUID id) {
        return jdbc.update("DELETE FROM notes WHERE id = ? AND user_id = ?", id, userId) > 0;
    }
}
