import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/** Servidor HTTP (do próprio Java) + tabela de rotas. */
public final class Main {
    private static final String ALLOWED_ORIGIN = System.getenv().getOrDefault("ALLOWED_ORIGIN", "*");

    public static void main(String[] args) throws Exception {
        Db.init();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", Main::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("API rodando na porta " + port);
    }

    private static void handle(HttpExchange ex) throws IOException {
        int status = 200;
        Object result;
        try {
            // CORS: permite o front (outro domínio, ex.: Netlify) chamar esta API.
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

            if (ex.getRequestMethod().equals("OPTIONS")) { // "pré-checagem" que o navegador faz
                ex.sendResponseHeaders(204, -1);
                ex.close();
                return;
            }
            result = route(ex);
        } catch (ApiException e) {
            status = e.status;
            result = Map.of("error", e.getMessage());
        } catch (IllegalArgumentException e) { // JSON inválido
            status = 400;
            result = Map.of("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            status = 500;
            result = Map.of("error", "Erro interno do servidor.");
        }

        byte[] bytes = Json.write(result).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static Map<String, String> queryParams(String raw) {
        Map<String, String> q = new HashMap<>();
        if (raw == null) return q;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            q.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return q;
    }

    /** Descobre qual rota foi chamada e executa. */
    private static Object route(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] parts = ex.getRequestURI().getPath().replaceAll("^/+|/+$", "").split("/");

        if (parts.length == 1 && parts[0].isEmpty()) return Map.of("ok", true); // health check do servidor
        if (!parts[0].equals("api") || parts.length < 2) throw new ApiException(404, "Rota não encontrada.");

        // "PUT /api/notes/123" vira a chave "PUT notes/:id"
        String id = parts.length == 3 ? parts[2] : null;
        String key = method + " " + parts[1] + (id != null ? "/:id" : "");
        if (parts.length > 3) throw new ApiException(404, "Rota não encontrada.");

        // Rotas públicas (não exigem login)
        if (key.equals("GET health")) return Map.of("ok", true);
        if (parts[1].equals("auth") && method.equals("POST") && id != null) {
            Map<String, Object> body = body(ex);
            return switch (id) {
                case "register" -> Auth.register(body);
                case "login" -> Auth.login(body);
                default -> throw new ApiException(404, "Rota não encontrada.");
            };
        }

        // Daqui para baixo: precisa estar logado
        UUID user = Auth.userFromHeader(ex.getRequestHeaders().getFirst("Authorization"));
        Map<String, String> q = queryParams(ex.getRequestURI().getRawQuery());

        return switch (key) {
            case "GET profile" -> Auth.getProfile(user);
            case "PUT profile" -> Auth.updateProfile(user, body(ex));
            case "GET dashboard" -> Finance.dashboard(user);

            case "GET transactions" -> Finance.listTransactions(user, q);
            case "POST transactions" -> Finance.createTransaction(user, body(ex));
            case "PUT transactions/:id" -> Finance.updateTransaction(user, id, body(ex));
            case "DELETE transactions/:id" -> Finance.deleteTransaction(user, id);

            case "GET fixed-expenses" -> Finance.listFixed(user);
            case "POST fixed-expenses" -> Finance.createFixed(user, body(ex));
            case "PUT fixed-expenses/:id" -> Finance.updateFixed(user, id, body(ex));
            case "DELETE fixed-expenses/:id" -> Finance.deleteFixed(user, id);

            case "GET notes" -> Finance.listNotes(user);
            case "POST notes" -> Finance.createNote(user, body(ex));
            case "PUT notes/:id" -> Finance.updateNote(user, id, body(ex));
            case "DELETE notes/:id" -> Finance.deleteNote(user, id);

            default -> throw new ApiException(404, "Rota não encontrada.");
        };
    }

    private static Map<String, Object> body(HttpExchange ex) throws IOException {
        byte[] raw = ex.getRequestBody().readNBytes(100_000);
        return Json.readObject(new String(raw, StandardCharsets.UTF_8));
    }
}
