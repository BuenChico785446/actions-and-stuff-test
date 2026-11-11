package dev.bedrockmotion.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {

    private final String s;
    private int p;

    private Json(String s) {
        this.s = s;
    }

    public static Object parse(String text) {
        Json j = new Json(text);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') j.p = 1;
        return j.value();
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, 0);
        return out.append('\n').toString();
    }

    private static void write(Object value, StringBuilder out, int depth) {
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                out.append("{}");
                return;
            }
            out.append("{\n");
            int left = map.size();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                indent(out, depth + 1);
                quote(String.valueOf(e.getKey()), out);
                out.append(": ");
                write(e.getValue(), out, depth + 1);
                out.append(--left > 0 ? ",\n" : "\n");
            }
            indent(out, depth);
            out.append('}');
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                out.append("[]");
                return;
            }
            out.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                indent(out, depth + 1);
                write(list.get(i), out, depth + 1);
                out.append(i < list.size() - 1 ? ",\n" : "\n");
            }
            indent(out, depth);
            out.append(']');
        } else if (value instanceof String str) {
            quote(str, out);
        } else if (value instanceof Number n) {
            double d = n.doubleValue();
            out.append(d == Math.rint(d) && Math.abs(d) < 1e15 ? String.valueOf((long) d) : String.valueOf(d));
        } else if (value instanceof Boolean b) {
            out.append(b);
        } else {
            out.append("null");
        }
    }

    private static void indent(StringBuilder out, int depth) {
        for (int i = 0; i < depth; i++) out.append("  ");
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object o) {
        return o instanceof List<?> l ? (List<Object>) l : null;
    }

    public static String str(Object o, String def) {
        return o instanceof String x ? x : def;
    }

    public static double num(Object o, double def) {
        if (o instanceof Double d) return d;
        if (o instanceof Boolean b) return b ? 1 : 0;
        return def;
    }

    public static boolean bool(Object o, boolean def) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Double d) return d != 0;
        return def;
    }

    private RuntimeException err(String m) {
        int line = 1;
        for (int i = 0; i < Math.min(p, s.length()); i++) if (s.charAt(i) == '\n') line++;
        return new IllegalArgumentException("JSON: " + m + " (line " + line + ")");
    }

    private void ws() {
        int n = s.length();
        while (p < n) {
            char c = s.charAt(p);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                p++;
            } else if (c == '/' && p + 1 < n && s.charAt(p + 1) == '/') {
                while (p < n && s.charAt(p) != '\n') p++;
            } else if (c == '/' && p + 1 < n && s.charAt(p + 1) == '*') {
                int e = s.indexOf("*/", p + 2);
                p = e < 0 ? n : e + 2;
            } else {
                break;
            }
        }
    }

    private Object value() {
        ws();
        if (p >= s.length()) throw err("unexpected end");
        char c = s.charAt(p);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> lit("true", Boolean.TRUE);
            case 'f' -> lit("false", Boolean.FALSE);
            case 'n' -> lit("null", null);
            default -> number();
        };
    }

    private Object lit(String w, Object v) {
        if (!s.startsWith(w, p)) throw err("invalid literal");
        p += w.length();
        return v;
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        p++;
        while (true) {
            ws();
            if (p >= s.length()) throw err("object not closed");
            char c = s.charAt(p);
            if (c == '}') {
                p++;
                return m;
            }
            if (c == ',') {
                p++;
                continue;
            }
            if (c != '"') throw err("key expected");
            String k = string();
            ws();
            if (p >= s.length() || s.charAt(p) != ':') throw err("':' expected");
            p++;
            m.put(k, value());
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        p++;
        while (true) {
            ws();
            if (p >= s.length()) throw err("array not closed");
            char c = s.charAt(p);
            if (c == ']') {
                p++;
                return l;
            }
            if (c == ',') {
                p++;
                continue;
            }
            l.add(value());
        }
    }

    private String string() {
        p++;
        int start = p;
        int n = s.length();
        while (p < n) {
            char c = s.charAt(p);
            if (c == '"') {
                String r = s.substring(start, p);
                p++;
                return r;
            }
            if (c == '\\') break;
            p++;
        }
        StringBuilder sb = new StringBuilder(s.substring(start, Math.min(p, n)));
        while (p < n) {
            char c = s.charAt(p++);
            if (c == '"') return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (p >= n) break;
            char e = s.charAt(p++);
            switch (e) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> {
                    if (p + 4 > n) throw err("invalid \\u");
                    sb.append((char) Integer.parseInt(s.substring(p, p + 4), 16));
                    p += 4;
                }
                default -> sb.append(e);
            }
        }
        throw err("string not closed");
    }

    private Double number() {
        int start = p;
        int n = s.length();
        while (p < n) {
            char c = s.charAt(p);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') p++;
            else break;
        }
        if (start == p) throw err("unexpected character '" + s.charAt(p) + "'");
        try {
            return Double.valueOf(s.substring(start, p));
        } catch (NumberFormatException e) {
            throw err("invalid number");
        }
    }
}
