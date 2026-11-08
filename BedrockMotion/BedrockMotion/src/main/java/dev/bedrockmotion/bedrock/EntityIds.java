package dev.bedrockmotion.bedrock;

import java.util.List;
import java.util.Map;

public final class EntityIds {

    private static final Map<String, List<String>> JAVA_TO_BEDROCK = Map.ofEntries(
            Map.entry("minecraft:villager", List.of("minecraft:villager_v2")),
            Map.entry("minecraft:zombie_villager", List.of("minecraft:zombie_villager_v2")),
            Map.entry("minecraft:evoker", List.of("minecraft:evocation_illager")),
            Map.entry("minecraft:evoker_fangs", List.of("minecraft:evocation_fang")),
            Map.entry("minecraft:tropical_fish", List.of("minecraft:tropicalfish")),
            Map.entry("minecraft:zombified_piglin", List.of("minecraft:zombie_pigman")),
            Map.entry("minecraft:end_crystal", List.of("minecraft:ender_crystal")),
            Map.entry("minecraft:eye_of_ender", List.of("minecraft:eye_of_ender_signal")),
            Map.entry("minecraft:firework_rocket", List.of("minecraft:fireworks_rocket")),
            Map.entry("minecraft:fishing_bobber", List.of("minecraft:fishing_hook")),
            Map.entry("minecraft:experience_orb", List.of("minecraft:xp_orb")),
            Map.entry("minecraft:experience_bottle", List.of("minecraft:xp_bottle")),
            Map.entry("minecraft:potion", List.of("minecraft:splash_potion", "minecraft:lingering_potion")),
            Map.entry("minecraft:trident", List.of("minecraft:thrown_trident")),
            Map.entry("minecraft:wind_charge", List.of("minecraft:wind_charge_projectile")),
            Map.entry("minecraft:breeze_wind_charge", List.of("minecraft:breeze_wind_charge_projectile")),
            Map.entry("minecraft:wither_skull", List.of("minecraft:wither_skull", "minecraft:wither_skull_dangerous")),
            Map.entry("minecraft:spectral_arrow", List.of("minecraft:arrow")),
            Map.entry("minecraft:glow_item_frame", List.of("minecraft:item_frame")),
            Map.entry("minecraft:leash_knot", List.of("minecraft:leash_knot")));

    private EntityIds() {}

    public static List<String> bedrockCandidates(String javaId) {
        return JAVA_TO_BEDROCK.getOrDefault(javaId, List.of());
    }

    public static ClientEntityDef find(Map<String, ClientEntityDef> defs, String javaId) {
        ClientEntityDef def = defs.get(javaId);
        if (def != null) return def;
        for (String alt : bedrockCandidates(javaId)) {
            def = defs.get(alt);
            if (def != null) return def;
        }
        return null;
    }
}
