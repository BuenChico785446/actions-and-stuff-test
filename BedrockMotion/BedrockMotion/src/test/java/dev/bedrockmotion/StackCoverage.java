package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.*;

public final class StackCoverage {
    public static void main(String[] args) throws Exception {
        try (PackFiles baseFiles = PackFiles.open(Path.of(args[0])); PackFiles addonFiles = PackFiles.open(Path.of(args[1]))) {
            long t0 = System.currentTimeMillis();
            BedrockPack base = BedrockPack.load("base_vanilla", baseFiles, null);
            System.out.println("base  loaded in " + (System.currentTimeMillis() - t0) + " ms: " + base.summary());
            for (String p : base.problems.subList(0, Math.min(8, base.problems.size()))) System.out.println("   base problem: " + p);
            BedrockPack addon = BedrockPack.load("addon", addonFiles, null);
            BedrockPack.stack(List.of(base, addon));
            Map<String, Set<String>> missing = new TreeMap<>();
            int refs = 0;
            List<ClientEntityDef> defs = new ArrayList<>(addon.entities.values());
            defs.addAll(addon.attachables.values());
            for (ClientEntityDef d : defs) {
                for (String id : d.animations.values()) {
                    refs++;
                    String l = id.toLowerCase(Locale.ROOT);
                    boolean ok = l.startsWith("controller.") ? addon.controller(l) != null : addon.animation(l) != null;
                    if (!ok) missing.computeIfAbsent((l.startsWith("controller.") ? "controller " : "animation ") + l, k -> new TreeSet<>()).add(d.identifier);
                }
                for (ClientEntityDef.Conditional c : d.renderControllers) {
                    refs++;
                    if (addon.renderController(c.name().toLowerCase(Locale.ROOT)) == null) missing.computeIfAbsent("render_controller " + c.name().toLowerCase(Locale.ROOT), k -> new TreeSet<>()).add(d.identifier);
                }
                for (String g : d.geometry.values()) {
                    refs++;
                    if (addon.geometry(g.toLowerCase(Locale.ROOT).split(":")[0]) == null) missing.computeIfAbsent("geometry " + g.toLowerCase(Locale.ROOT), k -> new TreeSet<>()).add(d.identifier);
                }
            }
            System.out.println("references checked: " + refs + ", unresolved: " + missing.size());
            missing.forEach((k, v) -> System.out.println("  " + k + "   <- " + v));
        }
    }
}
