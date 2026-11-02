package com.meufinanceiro.service;

import com.meufinanceiro.dto.Requests;
import com.meufinanceiro.exception.ApiException;
import com.meufinanceiro.model.Profile;
import com.meufinanceiro.model.SessionResponse;
import com.meufinanceiro.model.User;
import com.meufinanceiro.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Cadastro, login, token e perfil. */
@Service
public class UserService {
    private final UserRepository users;
    private final PasswordHasher hasher;

    public UserService(UserRepository users, PasswordHasher hasher) {
        this.users = users;
        this.hasher = hasher;
    }

    public SessionResponse register(Requests.Register body) {
        String name = Validate.text(body.name(), "Digite seu nome completo", 100);
        String email = Validate.email(body.email());
        String password = Validate.text(body.password(), "Digite sua senha", 200);
        if (password.length() < 6) throw new ApiException(400, "A senha precisa ter pelo menos 6 caracteres.");

        try {
            UUID id = users.create(name, email, hasher.hash(password));
            return startSession(id, new Profile(name, email));
        } catch (DuplicateKeyException e) {
            throw new ApiException(409, "Este e-mail já está cadastrado.");
        }
    }

    public SessionResponse login(Requests.Login body) {
        String email = Validate.email(body.email());
        String password = Validate.text(body.password(), "Digite sua senha", 200);

        User user = users.findByEmail(email).orElse(null);
        if (user == null || !hasher.matches(password, user.passwordHash())) {
            throw new ApiException(401, "E-mail ou senha inválidos.");
        }
        return startSession(UUID.fromString(user.id()), new Profile(user.name(), user.email()));
    }

    private SessionResponse startSession(UUID userId, Profile profile) {
        String token = hasher.newToken();
        users.createSession(hasher.sha256(token), userId);
        return new SessionResponse(token, profile);
    }

    /** Descobre o usuário a partir do cabeçalho "Authorization: Bearer <token>". */
    public UUID userIdFromHeader(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "Faça login para continuar.");
        }
        String token = header.substring(7).trim();
        return users.findUserIdByToken(hasher.sha256(token))
                .orElseThrow(() -> new ApiException(401, "Sessão expirada. Faça login novamente."));
    }

    public Profile getProfile(UUID userId) {
        return users.findProfile(userId);
    }

    public Profile updateProfile(UUID userId, Requests.ProfileUpdate body) {
        String name = Validate.text(body.name(), "Informe nome e e-mail.", 100);
        String email = Validate.email(body.email());
        try {
            users.updateProfile(userId, name, email);
        } catch (DuplicateKeyException e) {
            throw new ApiException(409, "Este e-mail já está em uso.");
        }
        return users.findProfile(userId);
    }
}
