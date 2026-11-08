package dev.bedrockmotion.pack;

import dev.bedrockmotion.json.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SoundDefs {

    public record Sound(String path, double volume, double pitch) {}

    private SoundDefs() {}

    public static Map<String, List<Sound>> parse(Object json) {
        Map<String, List<Sound>> out = new LinkedHashMap<>();
        Map<String, Object> root = Json.obj(json);
        if (root == null) return out;
        Map<String, Object> defs = Json.obj(root.get("sound_definitions"));
        if (defs == null) defs = root;
        for (Map.Entry<String, Object> e : defs.entrySet()) {
            Map<String, Object> def = Json.obj(e.getValue());
            List<Object> sounds = def == null ? null : Json.arr(def.get("sounds"));
            if (sounds == null) continue;
            List<Sound> list = new ArrayList<>();
            for (Object o : sounds) {
                if (o instanceof String s) {
                    list.add(new Sound(s, 1, 1));
                } else if (Json.obj(o) != null) {
                    Map<String, Object> m = Json.obj(o);
                    String name = Json.str(m.get("name"), null);
                    if (name != null) list.add(new Sound(name, Json.num(m.get("volume"), 1), Json.num(m.get("pitch"), 1)));
                }
            }
            if (!list.isEmpty()) out.put(e.getKey(), list);
        }
        return out;
    }

    public static String javaSoundName(String bedrockPath) {
        String p = bedrockPath;
        if (p.startsWith("sounds/")) p = p.substring(7);
        int dot = p.lastIndexOf('.');
        if (dot > p.lastIndexOf('/')) p = p.substring(0, dot);
        return PathMapper.sanitize(p);
    }
}
