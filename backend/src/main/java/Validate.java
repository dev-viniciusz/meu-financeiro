import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Funções pequenas para validar os campos que chegam do front. */
final class Validate {
    private Validate() {}

    /** Texto obrigatório (sem espaços nas pontas). */
    static String text(Map<String, Object> body, String key, String message, int max) {
        Object v = body.get(key);
        String s = v == null ? "" : v.toString().trim();
        if (s.isEmpty()) throw new ApiException(400, message);
        if (s.length() > max) throw new ApiException(400, "Texto muito longo em \"" + key + "\".");
        return s;
    }

    /** Texto opcional: devolve "" se não vier nada. */
    static String optionalText(Map<String, Object> body, String key, int max) {
        Object v = body.get(key);
        String s = v == null ? "" : v.toString().trim();
        if (s.length() > max) throw new ApiException(400, "Texto muito longo em \"" + key + "\".");
        return s;
    }

    static String email(Map<String, Object> body) {
        String email = text(body, "email", "Digite um e-mail válido", 200).toLowerCase();
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new ApiException(400, "Digite um e-mail válido");
        return email;
    }

    /** Valor em centavos (número inteiro maior que zero). */
    static long cents(Map<String, Object> body) {
        Object v = body.get("amountCents");
        if (!(v instanceof Long cents) || cents <= 0 || cents > 100_000_000_000L) {
            throw new ApiException(400, "Informe um valor maior que zero.");
        }
        return cents;
    }

    /** Data no formato AAAA-MM-DD. Se não for obrigatória e vier vazia, devolve null. */
    static LocalDate date(Map<String, Object> body, String key, boolean required) {
        Object v = body.get(key);
        String s = v == null ? "" : v.toString().trim();
        if (s.isEmpty()) {
            if (required) throw new ApiException(400, "Preencha a descrição e a data.");
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "Data inválida.");
        }
    }

    static java.util.UUID id(String text) {
        try {
            return java.util.UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            throw new ApiException(404, "Registro não encontrado.");
        }
    }
}
