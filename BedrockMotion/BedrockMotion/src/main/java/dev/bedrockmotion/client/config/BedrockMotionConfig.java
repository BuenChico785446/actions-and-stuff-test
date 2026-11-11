package dev.bedrockmotion.client.config;

import dev.bedrockmotion.client.BedrockMotionClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = BedrockMotionClient.MOD_ID)
public class BedrockMotionConfig implements ConfigData {

    public static final double MIN_PARTICLE_SCALE = 0.05;
    public static final double MAX_PARTICLE_SCALE = 8.0;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean replacePlayers = false;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public double particleScale = 1.0;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public List<String> basePacks = new ArrayList<>();

    @ConfigEntry.Category("features")
    @ConfigEntry.Gui.TransitiveObject
    public Features features = new Features();

    @ConfigEntry.Gui.Excluded
    public Map<String, String> subpacks = new LinkedHashMap<>();

    @ConfigEntry.Gui.Excluded
    public Map<String, Map<String, Object>> settings = new LinkedHashMap<>();

    public static class Features {
        @ConfigEntry.Gui.Tooltip
        public boolean entities = true;

        @ConfigEntry.Gui.Tooltip
        public boolean driver = true;

        @ConfigEntry.Gui.Tooltip
        public boolean attachables = true;

        @ConfigEntry.Gui.Tooltip
        public boolean equipment = true;

        @ConfigEntry.Gui.Tooltip
        public boolean particles = true;

        @ConfigEntry.Gui.Tooltip
        public boolean textureMeshes = true;

        @ConfigEntry.Gui.Tooltip
        public boolean sounds = true;
    }

    @Override
    public void validatePostLoad() {
        if (features == null) features = new Features();
        if (basePacks == null) basePacks = new ArrayList<>();
        if (subpacks == null) subpacks = new LinkedHashMap<>();
        if (settings == null) settings = new LinkedHashMap<>();
        particleScale = clampedParticleScale();
    }

    public double clampedParticleScale() {
        if (Double.isNaN(particleScale)) return 1.0;
        return Math.max(MIN_PARTICLE_SCALE, Math.min(MAX_PARTICLE_SCALE, particleScale));
    }

    public Set<String> basePackNames() {
        Set<String> names = new LinkedHashSet<>();
        if (basePacks == null) return names;
        for (String name : basePacks) {
            if (name != null && !name.isBlank()) names.add(name.trim());
        }
        return names;
    }

    public String subpackFor(String pack) {
        return subpacks == null ? null : subpacks.get(pack);
    }

    public Map<String, Object> settingsFor(String pack) {
        Map<String, Object> mine = settings == null ? null : settings.get(pack);
        return mine == null ? Map.of() : mine;
    }

    public String packSignature() {
        return enabled + "|" + basePackNames() + "|" + subpacks + "|" + settings;
    }
}
