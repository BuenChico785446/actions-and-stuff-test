package dev.bedrockmotion.bedrock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class StackReport {

    private StackReport() {}

    public static List<String> unresolved(Collection<ClientEntityDef> defs, int limit) {
        Map<String, Set<String>> missing = new TreeMap<>();
        for (ClientEntityDef d : defs) {
            for (String id : d.animations.values()) {
                String lower = id.toLowerCase(Locale.ROOT);
                boolean ok = lower.startsWith("controller.") ? d.pack.controller(lower) != null : d.pack.animation(lower) != null;
                if (!ok) missing.computeIfAbsent((lower.startsWith("controller.") ? "controller " : "animation ") + lower, k -> new TreeSet<>()).add(d.identifier);
            }
            for (ClientEntityDef.Conditional c : d.renderControllers) {
                String lower = c.name().toLowerCase(Locale.ROOT);
                if (d.pack.renderController(lower) == null) missing.computeIfAbsent("render_controller " + lower, k -> new TreeSet<>()).add(d.identifier);
            }
            for (String g : d.geometry.values()) {
                String lower = g.toLowerCase(Locale.ROOT).split(":")[0];
                if (d.pack.geometry(lower) == null) missing.computeIfAbsent("geometry " + lower, k -> new TreeSet<>()).add(d.identifier);
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add(missing.size() + " referenced id(s) are not defined by any loaded pack");
        for (Map.Entry<String, Set<String>> e : missing.entrySet()) {
            if (lines.size() > limit) break;
            lines.add("  " + e.getKey() + "  <- " + e.getValue());
        }
        return lines;
    }
}
