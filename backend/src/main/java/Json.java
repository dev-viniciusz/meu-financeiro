import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mini conversor de JSON (para não precisar de biblioteca externa).
 * Objeto JSON <-> Map, array <-> List, texto <-> String, número <-> Long/Double.
 */
final class Json {
    private Json() {}

    // ---------- Java -> texto JSON ----------

    static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb);
        return sb.toString();
    }

    private static void write(Object v, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                write(e.getValue(), sb);
            }
            sb.append('}');
        } else if (v instanceof List<?> list) {
            sb.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                write(list.get(i), sb);
            }
            sb.append(']');
        } else {
            writeString(v.toString(), sb);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ---------- texto JSON -> Java ----------

    private static final class Reader {
        final String s;
        int i = 0;

        Reader(String s) { this.s = s; }

        void skipSpaces() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        IllegalArgumentException error() {
            return new IllegalArgumentException("JSON inválido");
        }

        Object value() {
            skipSpaces();
            if (i >= s.length()) throw error();
            char c = s.charAt(i);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            return number();
        }

        Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            i++; // {
            skipSpaces();
            if (i < s.length() && s.charAt(i) == '}') { i++; return map; }
            while (true) {
                skipSpaces();
                if (i >= s.length() || s.charAt(i) != '"') throw error();
                String key = string();
                skipSpaces();
                if (i >= s.length() || s.charAt(i) != ':') throw error();
                i++;
                map.put(key, value());
                skipSpaces();
                if (i >= s.length()) throw error();
                char c = s.charAt(i++);
                if (c == '}') return map;
                if (c != ',') throw error();
            }
        }

        List<Object> array() {
            List<Object> list = new ArrayList<>();
            i++; // [
            skipSpaces();
            if (i < s.length() && s.charAt(i) == ']') { i++; return list; }
            while (true) {
                list.add(value());
                skipSpaces();
                if (i >= s.length()) throw error();
                char c = s.charAt(i++);
                if (c == ']') return list;
                if (c != ',') throw error();
            }
        }

        String string() {
            StringBuilder sb = new StringBuilder();
            i++; // "
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                if (i >= s.length()) throw error();
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw error();
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> sb.append(e); // \" \\ \/
                }
            }
            throw error();
        }

        Number number() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String text = s.substring(start, i);
            if (text.isEmpty()) throw error();
            if (text.contains(".") || text.contains("e") || text.contains("E")) return Double.parseDouble(text);
            return Long.parseLong(text);
        }
    }

    /** Lê um JSON que deve ser um objeto ({ ... }) e devolve como Map. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> readObject(String text) {
        try {
            Reader r = new Reader(text);
            Object v = r.value();
            r.skipSpaces();
            if (!(v instanceof Map) || r.i != text.length()) throw r.error();
            return (Map<String, Object>) v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("JSON inválido");
        }
    }
}
