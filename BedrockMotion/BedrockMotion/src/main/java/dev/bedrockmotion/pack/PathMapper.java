package dev.bedrockmotion.pack;

import java.util.Locale;

public final class PathMapper {

    public static final String ROOT = "bedrockmotion/";

    private PathMapper() {}

    public static String sanitize(String path) {
        String p = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(p.length());
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '_' || c == '-';
            sb.append(ok ? c : '_');
        }
        return sb.toString();
    }
}
