package com.meufinanceiro.repository;

import com.meufinanceiro.model.Profile;
import com.meufinanceiro.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Acesso às tabelas users e sessions. */
@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Cria o usuário e devolve o id. Lança DuplicateKeyException se o e-mail já existe. */
    public UUID create(String name, String email, String passwordHash) {
        String id = jdbc.queryForObject(
                "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?) RETURNING id::text",
                String.class, name, email, passwordHash);
        return UUID.fromString(id);
    }

    public Optional<User> findByEmail(String email) {
        return jdbc.query("SELECT id::text AS id, name, email, password_hash FROM users WHERE email = ?",
                        (rs, i) -> new User(rs.getString("id"), rs.getString("name"),
                                rs.getString("email"), rs.getString("password_hash")),
                        email)
                .stream().findFirst();
    }

    public Profile findProfile(UUID id) {
        return jdbc.queryForObject("SELECT name, email FROM users WHERE id = ?",
                (rs, i) -> new Profile(rs.getString("name"), rs.getString("email")), id);
    }

    public void updateProfile(UUID id, String name, String email) {
        jdbc.update("UPDATE users SET name = ?, email = ? WHERE id = ?", name, email, id);
    }

    public void createSession(String tokenHash, UUID userId) {
        jdbc.update("INSERT INTO sessions (token_hash, user_id) VALUES (?, ?)", tokenHash, userId);
    }

    public Optional<UUID> findUserIdByToken(String tokenHash) {
        return jdbc.query("SELECT user_id::text FROM sessions WHERE token_hash = ? "
                                + "AND created_at > now() - interval '30 days'",
                        (rs, i) -> UUID.fromString(rs.getString(1)), tokenHash)
                .stream().findFirst();
    }
}
