package dev.bedrockmotion.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

public final class ConfigTest {

    private static final String OLD_FILE = """
            {
              "enabled": true,
              "replacePlayers": true,
              "particleScale": 50,
              "features": {"entities": true, "driver": false, "sounds": false},
              "basePacks": ["MCBVanillaResourcePack", " ", "other.zip"],
              "subpacks": {"Smooth Moves.zip": "high_quality"},
              "settings": {"Smooth Moves.zip": {"toolbelt": true, "style": "wide", "count": 3}}
            }
            """;

    private static int failures;

    private static void check(String what, Object actual, Object expected) {
        if (!String.valueOf(actual).equals(String.valueOf(expected))) {
            failures++;
            System.out.println("ERROR " + what + ": " + actual + ", expected " + expected);
        }
    }

    public static void main(String[] args) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        BedrockMotionConfig c = gson.fromJson(OLD_FILE, BedrockMotionConfig.class);
        c.validatePostLoad();
        check("replacePlayers", c.replacePlayers, true);
        check("particle scale clamped to max", c.particleScale, BedrockMotionConfig.MAX_PARTICLE_SCALE);
        check("feature driver off", c.features.driver, false);
        check("feature sounds off", c.features.sounds, false);
        check("feature missing in file stays on", c.features.attachables, true);
        check("base packs trimmed and blank dropped", c.basePackNames(), "[MCBVanillaResourcePack, other.zip]");
        check("subpack", c.subpackFor("Smooth Moves.zip"), "high_quality");
        check("subpack unknown", c.subpackFor("x.zip"), null);
        Map<String, Object> mine = c.settingsFor("Smooth Moves.zip");
        check("setting toggle", mine.get("toolbelt"), true);
        check("setting string", mine.get("style"), "wide");
        check("settings of unknown pack", c.settingsFor("x.zip"), "{}");

        BedrockMotionConfig empty = gson.fromJson("{}", BedrockMotionConfig.class);
        empty.validatePostLoad();
        check("empty file: enabled", empty.enabled, true);
        check("empty file: replacePlayers", empty.replacePlayers, false);
        check("empty file: scale", empty.particleScale, 1.0);
        check("empty file: features", empty.features.entities && empty.features.textureMeshes, true);

        BedrockMotionConfig nulls = gson.fromJson("{\"features\":null,\"basePacks\":null,\"subpacks\":null,\"settings\":null,\"particleScale\":0}", BedrockMotionConfig.class);
        nulls.validatePostLoad();
        check("null sections repaired", nulls.features != null && nulls.basePacks != null && nulls.subpacks != null && nulls.settings != null, true);
        check("scale lower bound", nulls.particleScale, BedrockMotionConfig.MIN_PARTICLE_SCALE);
        check("null base packs usable", nulls.basePackNames(), "[]");

        BedrockMotionConfig unvalidated = new BedrockMotionConfig();
        unvalidated.features = null;
        unvalidated.settings = null;
        unvalidated.subpacks = null;
        unvalidated.basePacks = null;
        unvalidated.particleScale = Double.NaN;
        check("nan scale", unvalidated.clampedParticleScale(), 1.0);
        check("helpers survive nulls", unvalidated.settingsFor("a") + " " + unvalidated.subpackFor("a") + " " + unvalidated.basePackNames(), "{} null []");

        String signature = c.packSignature();
        c.particleScale = 2;
        c.features.sounds = true;
        c.replacePlayers = false;
        check("runtime-only changes keep the signature", c.packSignature(), signature);
        c.basePacks.add("third.zip");
        check("base pack change alters signature", c.packSignature().equals(signature), false);
        String withBase = c.packSignature();
        c.enabled = false;
        check("enabled change alters signature", c.packSignature().equals(withBase), false);
        String disabled = c.packSignature();
        c.settings.get("Smooth Moves.zip").put("toolbelt", false);
        check("pack setting change alters signature", c.packSignature().equals(disabled), false);
        String settingsChanged = c.packSignature();
        c.subpacks.put("Smooth Moves.zip", "low");
        check("subpack change alters signature", c.packSignature().equals(settingsChanged), false);

        String written = gson.toJson(c);
        BedrockMotionConfig back = gson.fromJson(written, BedrockMotionConfig.class);
        back.validatePostLoad();
        check("round trip", back.packSignature(), c.packSignature());
        check("file keeps the old key names", written.contains("\"replacePlayers\"") && written.contains("\"particleScale\"")
                && written.contains("\"features\"") && written.contains("\"basePacks\"") && written.contains("\"subpacks\"")
                && written.contains("\"settings\""), true);

        for (Field f : BedrockMotionConfig.class.getFields()) {
            if (f.getName().startsWith("MIN_") || f.getName().startsWith("MAX_")) continue;
            boolean hidden = f.isAnnotationPresent(ConfigEntry.Gui.Excluded.class);
            boolean categorized = f.isAnnotationPresent(ConfigEntry.Category.class);
            check("field " + f.getName() + " is either in a category or excluded", hidden != categorized, true);
        }
        for (Field f : BedrockMotionConfig.Features.class.getFields()) {
            check("feature " + f.getName() + " has a tooltip", f.isAnnotationPresent(ConfigEntry.Gui.Tooltip.class), true);
        }

        Set<String> langKeys = LangKeys.read("src/main/resources/assets/bedrockmotion/lang/en_us.json");
        for (Field f : BedrockMotionConfig.class.getFields()) {
            if (f.getName().startsWith("MIN_") || f.getName().startsWith("MAX_") || f.isAnnotationPresent(ConfigEntry.Gui.Excluded.class)) continue;
            if (f.isAnnotationPresent(ConfigEntry.Gui.TransitiveObject.class)) {
                for (Field inner : f.getType().getFields()) {
                    String key = "text.autoconfig.bedrockmotion.option." + f.getName() + "." + inner.getName();
                    check(key, langKeys.contains(key), true);
                    check(key + ".@Tooltip", langKeys.contains(key + ".@Tooltip"), true);
                }
                String category = f.getAnnotation(ConfigEntry.Category.class).value();
                check("category " + category, langKeys.contains("text.autoconfig.bedrockmotion.category." + category), true);
                continue;
            }
            String key = "text.autoconfig.bedrockmotion.option." + f.getName();
            check(key, langKeys.contains(key), true);
            if (f.isAnnotationPresent(ConfigEntry.Gui.Tooltip.class)) check(key + ".@Tooltip", langKeys.contains(key + ".@Tooltip"), true);
            String category = f.getAnnotation(ConfigEntry.Category.class).value();
            check("category " + category, langKeys.contains("text.autoconfig.bedrockmotion.category." + category), true);
        }
        check("title", langKeys.contains("text.autoconfig.bedrockmotion.title"), true);

        System.out.println(failures == 0 ? "ConfigTest: all tests passed" : "ConfigTest: " + failures + " errors");
        if (failures > 0) System.exit(1);
    }

    private static final class LangKeys {
        static Set<String> read(String path) throws Exception {
            String text = java.nio.file.Files.readString(java.nio.file.Path.of(path));
            Map<?, ?> map = new Gson().fromJson(text, Map.class);
            @SuppressWarnings("unchecked")
            Set<String> keys = (Set<String>) (Set<?>) map.keySet();
            return keys;
        }
    }
}
