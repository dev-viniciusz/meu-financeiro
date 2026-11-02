import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cadastro, login e perfil. Usa só classes do próprio Java (PBKDF2 para senha, token aleatório). */
final class Auth {
    private Auth() {}

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 120_000;

    // ---------- senha ----------

    private static byte[] pbkdf2(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    private static String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);
            Base64.Encoder b64 = Base64.getEncoder();
            return b64.encodeToString(salt) + ":" + b64.encodeToString(pbkdf2(password, salt));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean checkPassword(String password, String stored) {
        try {
            String[] parts = stored.split(":");
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            return MessageDigest.isEqual(expected, pbkdf2(password, salt));
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- token de sessão ----------

    private static String sha256(String text) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(text.getBytes());
            return HexFormat.of().formatHex(h);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String createSession(String userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        // No banco guardamos só o hash do token (se o banco vazar, os tokens não vazam).
        Db.update("INSERT INTO sessions (token_hash, user_id) VALUES (?, ?)", sha256(token), UUID.fromString(userId));
        return token;
    }

    /** Descobre quem é o usuário a partir do cabeçalho "Authorization: Bearer <token>". */
    static UUID userFromHeader(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "Faça login para continuar.");
        }
        String token = header.substring(7).trim();
        List<Map<String, Object>> rows = Db.query(
                "SELECT user_id AS id FROM sessions WHERE token_hash = ? AND created_at > now() - interval '30 days'",
                sha256(token));
        if (rows.isEmpty()) throw new ApiException(401, "Sessão expirada. Faça login novamente.");
        return UUID.fromString((String) rows.get(0).get("id"));
    }

    // ---------- rotas ----------

    private static Map<String, Object> sessionResponse(String userId, String name, String email) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("name", name);
        profile.put("email", email);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", createSession(userId));
        out.put("profile", profile);
        return out;
    }

    static Object register(Map<String, Object> body) {
        String name = Validate.text(body, "name", "Digite seu nome completo", 100);
        String email = Validate.email(body);
        String password = Validate.text(body, "password", "Digite sua senha", 200);
        if (password.length() < 6) throw new ApiException(400, "A senha precisa ter pelo menos 6 caracteres.");

        try {
            List<Map<String, Object>> rows = Db.query(
                    "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?) RETURNING id",
                    name, email, hashPassword(password));
            return sessionResponse((String) rows.get(0).get("id"), name, email);
        } catch (RuntimeException e) {
            if (Db.isDuplicate(e)) throw new ApiException(409, "Este e-mail já está cadastrado.");
            throw e;
        }
    }

    static Object login(Map<String, Object> body) {
        String email = Validate.email(body);
        String password = Validate.text(body, "password", "Digite sua senha", 200);

        List<Map<String, Object>> rows = Db.query(
                "SELECT id, name, email, password_hash FROM users WHERE email = ?", email);
        if (rows.isEmpty() || !checkPassword(password, (String) rows.get(0).get("password_hash"))) {
            throw new ApiException(401, "E-mail ou senha inválidos.");
        }
        Map<String, Object> u = rows.get(0);
        return sessionResponse((String) u.get("id"), (String) u.get("name"), (String) u.get("email"));
    }

    static Object getProfile(UUID userId) {
        return Db.query("SELECT name, email FROM users WHERE id = ?", userId).get(0);
    }

    static Object updateProfile(UUID userId, Map<String, Object> body) {
        String name = Validate.text(body, "name", "Informe nome e e-mail.", 100);
        String email = Validate.email(body);
        try {
            Db.update("UPDATE users SET name = ?, email = ? WHERE id = ?", name, email, userId);
        } catch (RuntimeException e) {
            if (Db.isDuplicate(e)) throw new ApiException(409, "Este e-mail já está em uso.");
            throw e;
        }
        return getProfile(userId);
    }
}
