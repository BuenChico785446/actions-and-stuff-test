package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.EntityIds;
import java.util.HashMap;
import java.util.Map;

public final class EntityIdsTest {

    private static int failures;

    private static ClientEntityDef def(String id) {
        ClientEntityDef d = new ClientEntityDef();
        d.identifier = id;
        return d;
    }

    private static void check(String what, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            failures++;
            System.out.println("ERROR " + what + ": expected " + expected + " but was " + actual);
        }
    }

    public static void main(String[] args) {
        Map<String, ClientEntityDef> defs = new HashMap<>();
        for (String id : new String[] {"minecraft:zombie_villager", "minecraft:zombie_villager_v2", "minecraft:villager", "minecraft:villager_v2", "minecraft:pig", "minecraft:evocation_illager"}) {
            defs.put(id, def(id));
        }
        check("zombie villager prefers v2", "minecraft:zombie_villager_v2", EntityIds.find(defs, "minecraft:zombie_villager").identifier);
        check("villager prefers v2", "minecraft:villager_v2", EntityIds.find(defs, "minecraft:villager").identifier);
        check("evoker maps", "minecraft:evocation_illager", EntityIds.find(defs, "minecraft:evoker").identifier);
        check("same id stays", "minecraft:pig", EntityIds.find(defs, "minecraft:pig").identifier);
        defs.remove("minecraft:zombie_villager_v2");
        check("falls back to the legacy id", "minecraft:zombie_villager", EntityIds.find(defs, "minecraft:zombie_villager").identifier);
        check("unknown", "none", EntityIds.find(defs, "minecraft:cow") == null ? "none" : "found");
        System.out.println(failures == 0 ? "EntityIdsTest: all tests passed" : "EntityIdsTest: " + failures + " errors");
        if (failures > 0) System.exit(1);
    }
}
