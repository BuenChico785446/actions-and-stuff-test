package dev.bedrockmotion.bedrock;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Definitions only present in base packs stay inactive, unless a pack above overrides something they use. */
public final class EntityStack {

    public record Result(Map<String, ClientEntityDef> entities, Map<String, ClientEntityDef> attachables) {}

    private EntityStack() {}

    public static Result build(List<BedrockPack> loaded) {
        Map<String, ClientEntityDef> entityChain = new HashMap<>();
        Map<String, ClientEntityDef> attachableChain = new HashMap<>();
        Map<String, ClientEntityDef> entities = new HashMap<>();
        Map<String, ClientEntityDef> attachables = new HashMap<>();
        for (BedrockPack p : loaded) {
            merge(p.entities, entityChain, entities, p.baseOnly);
            merge(p.attachables, attachableChain, attachables, p.baseOnly);
        }
        adoptOverridden(loaded, entityChain, entities);
        return new Result(entities, attachables);
    }

    private static void adoptOverridden(List<BedrockPack> loaded, Map<String, ClientEntityDef> chain, Map<String, ClientEntityDef> active) {
        for (Map.Entry<String, ClientEntityDef> e : chain.entrySet()) {
            if (active.containsKey(e.getKey())) continue;
            ClientEntityDef def = e.getValue();
            for (int i = loaded.size() - 1; i >= 0; i--) {
                BedrockPack p = loaded.get(i);
                if (p.baseOnly || !overrides(p, def)) continue;
                def.pack = p;
                active.put(e.getKey(), def);
                break;
            }
        }
    }

    // Geometry alone does not count: such defs (boats, tridents) may not render, and the pose driver would then reset the Java model.
    private static boolean overrides(BedrockPack pack, ClientEntityDef def) {
        for (String id : def.animations.values()) {
            String key = id.toLowerCase(Locale.ROOT);
            if (pack.animations.containsKey(key) || pack.controllers.containsKey(key)) return true;
        }
        for (ClientEntityDef.Conditional c : def.renderControllers) {
            if (pack.renderControllers.containsKey(c.name().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static void merge(
            Map<String, ClientEntityDef> from,
            Map<String, ClientEntityDef> chain,
            Map<String, ClientEntityDef> active,
            boolean baseOnly) {
        for (Map.Entry<String, ClientEntityDef> e : from.entrySet()) {
            ClientEntityDef def = e.getValue();
            ClientEntityDef lower = chain.get(e.getKey());
            if (lower != null) def.inheritMissing(lower);
            chain.put(e.getKey(), def);
            if (!baseOnly) active.put(e.getKey(), def);
        }
    }
}
