package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.molang.PackSettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PackSettingsData implements PackSettings {

    private final Map<String, List<String>> choices = new HashMap<>();
    private final Map<String, Boolean> toggles = new HashMap<>();

    public static PackSettingsData fromManifest(Object manifestJson) {
        PackSettingsData s = new PackSettingsData();
        Map<String, Object> m = Json.obj(manifestJson);
        List<Object> settings = m == null ? null : Json.arr(m.get("settings"));
        if (settings == null) return s;
        for (Object o : settings) {
            Map<String, Object> e = Json.obj(o);
            if (e == null || !(e.get("name") instanceof String name)) continue;
            String key = name.toLowerCase(Locale.ROOT);
            String type = Json.str(e.get("type"), "");
            if (type.equals("toggle")) {
                s.toggles.put(key, Json.bool(e.get("default"), false));
            } else if (type.equals("multiselect")) {
                List<String> sel = new ArrayList<>();
                List<Object> defs = Json.arr(e.get("defaults"));
                if (defs != null) for (Object d : defs) if (d instanceof String x) sel.add(x);
                s.choices.put(key, sel);
            } else {
                List<String> sel = new ArrayList<>();
                if (e.get("default") instanceof String d) sel.add(d);
                s.choices.put(key, sel);
            }
        }
        return s;
    }

    public void override(String name, String option) {
        choices.put(name.toLowerCase(Locale.ROOT), new ArrayList<>(List.of(option)));
    }

    public void overrideToggle(String name, boolean value) {
        toggles.put(name.toLowerCase(Locale.ROOT), value);
    }

    @Override
    public boolean selected(String name, String option) {
        List<String> sel = choices.get(name.toLowerCase(Locale.ROOT));
        return sel != null && sel.contains(option);
    }

    @Override
    public boolean enabled(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        Boolean t = toggles.get(key);
        if (t != null) return t;
        List<String> sel = choices.get(key);
        return sel != null && sel.contains("enabled");
    }

    public int size() {
        return choices.size() + toggles.size();
    }
}
