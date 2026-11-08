package dev.bedrockmotion.bedrock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Definitions only present in base packs stay inactive; the Java game already renders those models. */
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
        return new Result(entities, attachables);
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
