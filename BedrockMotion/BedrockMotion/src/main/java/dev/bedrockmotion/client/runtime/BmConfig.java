package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.bedrock.ParticleSystem;
import dev.bedrockmotion.client.BedrockMotionClient;
import dev.bedrockmotion.json.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class BmConfig {

    public static final List<String> FEATURES =
            List.of("entities", "driver", "attachables", "equipment", "particles", "textureMeshes", "sounds");

    public static volatile boolean enabled = true;

    private BmConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("bedrockmotion.json");
    }

    static Map<String, Object> read() {
        Path file = file();
        if (!Files.isRegularFile(file)) return null;
        try {
            return Json.obj(Json.parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)));
        } catch (IOException | RuntimeException e) {
            BedrockMotionClient.LOGGER.warn("config/bedrockmotion.json invalid: {}", e.getMessage());
            return null;
        }
    }

    public static void save() {
        Map<String, Object> config = read();
        Map<String, Object> out = config == null ? new LinkedHashMap<>() : new LinkedHashMap<>(config);
        out.put("enabled", enabled);
        out.put("replacePlayers", BmRuntime.replacePlayers);
        out.put("particleScale", ParticleSystem.sizeFactor);
        Map<String, Object> features = new LinkedHashMap<>();
        for (String name : FEATURES) features.put(name, BmRuntime.features.get(name));
        out.put("features", features);
        try {
            Files.writeString(file(), Json.write(out), StandardCharsets.UTF_8);
        } catch (IOException e) {
            BedrockMotionClient.LOGGER.warn("config/bedrockmotion.json could not be saved: {}", e.getMessage());
        }
    }
}
