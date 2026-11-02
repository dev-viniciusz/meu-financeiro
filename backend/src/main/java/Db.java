import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/** Conexão com o PostgreSQL: um "pool" simples + funções para rodar SQL. */
final class Db {
    private Db() {}

    private static final int POOL_SIZE = 4; // planos gratuitos de banco têm poucas conexões
    private static final Semaphore permits = new Semaphore(POOL_SIZE);
    private static final Deque<Connection> idle = new ArrayDeque<>();
    private static String jdbcUrl;
    private static final Properties props = new Properties();

    /**
     * Lê DATABASE_URL. Aceita os dois formatos:
     *   postgresql://usuario:senha@host:5432/banco?sslmode=require   (padrão de Neon/Render/Supabase)
     *   jdbc:postgresql://host:5432/banco
     */
    static void init() throws Exception {
        String url = System.getenv("DATABASE_URL");
        if (url == null || url.isBlank()) {
            url = "postgresql://postgres@localhost:5432/financeiro";
        }
        if (url.startsWith("jdbc:")) {
            jdbcUrl = url;
        } else {
            URI uri = URI.create(url.replaceFirst("^postgres(ql)?://", "http://"));
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath()
                    + (uri.getQuery() != null ? "?" + uri.getQuery() : "");
            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":", 2);
                props.setProperty("user", java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
                if (parts.length > 1) {
                    props.setProperty("password", java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                }
            }
        }
        createTables();
    }

    private static void createTables() throws Exception {
        try (InputStream in = Db.class.getResourceAsStream("/schema.sql")) {
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Connection c = borrow();
            try (Statement st = c.createStatement()) {
                for (String command : sql.split(";\\s*\\n")) {
                    if (!command.isBlank()) st.execute(command);
                }
            } finally {
                release(c);
            }
        }
    }

    private static Connection borrow() throws SQLException {
        permits.acquireUninterruptibly();
        try {
            while (true) {
                Connection c;
                synchronized (idle) { c = idle.poll(); }
                if (c == null) return DriverManager.getConnection(jdbcUrl, props);
                if (c.isValid(2)) return c;
                try { c.close(); } catch (SQLException ignored) { }
            }
        } catch (SQLException | RuntimeException e) {
            permits.release();
            throw e;
        }
    }

    private static void release(Connection c) {
        synchronized (idle) { idle.push(c); }
        permits.release();
    }

    private static PreparedStatement prepare(Connection c, String sql, Object[] params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
        return ps;
    }

    /** SELECT: devolve uma lista de linhas; cada linha é um Map coluna -> valor. */
    static List<Map<String, Object>> query(String sql, Object... params) {
        try {
            Connection c = borrow();
            try (PreparedStatement ps = prepare(c, sql, params); ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        Object v = rs.getObject(i);
                        row.put(meta.getColumnLabel(i), v instanceof UUID ? v.toString() : v);
                    }
                    rows.add(row);
                }
                return rows;
            } finally {
                release(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** INSERT/UPDATE/DELETE: devolve quantas linhas foram afetadas. */
    static int update(String sql, Object... params) {
        try {
            Connection c = borrow();
            try (PreparedStatement ps = prepare(c, sql, params)) {
                return ps.executeUpdate();
            } finally {
                release(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** True se o erro é "valor duplicado" (ex.: e-mail já cadastrado). */
    static boolean isDuplicate(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException s && "23505".equals(s.getSQLState())) return true;
        }
        return false;
    }
}
