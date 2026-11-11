package dev.bedrockmotion;

import dev.bedrockmotion.json.Json;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonWriteTest {
    public static void main(String[] args) {
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("entities", true);
        features.put("sounds", false);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("enabled", true);
        config.put("particleScale", 0.5);
        config.put("count", 3.0);
        config.put("name", "a \"quoted\" \\ path\n");
        config.put("basePacks", List.of("base_vanilla", "x"));
        config.put("empty", new LinkedHashMap<>());
        config.put("features", features);
        String text = Json.write(config);
        Map<String, Object> back = Json.obj(Json.parse(text));
        boolean ok = back != null
                && Json.bool(back.get("enabled"), false)
                && Json.num(back.get("particleScale"), 0) == 0.5
                && Json.num(back.get("count"), 0) == 3
                && "a \"quoted\" \\ path\n".equals(Json.str(back.get("name"), ""))
                && Json.arr(back.get("basePacks")).size() == 2
                && !Json.bool(Json.obj(back.get("features")).get("sounds"), true);
        System.out.print(text);
        System.out.println(ok ? "JsonWriteTest: ok" : "JsonWriteTest: FAILED");
        if (!ok) System.exit(1);
    }
}
