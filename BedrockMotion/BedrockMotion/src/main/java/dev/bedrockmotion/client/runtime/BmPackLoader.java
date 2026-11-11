package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.client.BedrockMotionClient;
import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.molang.Queries;
import dev.bedrockmotion.pack.PackFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

public final class BmPackLoader implements PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(
            SharedState sharedState,
            Executor prepExecutor,
            PreparationBarrier preparationBarrier,
            Executor applicationExecutor) {
        ResourceManager manager = sharedState.resourceManager();
        CompletableFuture<List<BedrockPack>> loaded = CompletableFuture.supplyAsync(() -> loadAll(manager), prepExecutor);
        return loaded.thenCompose(preparationBarrier::wait).thenAcceptAsync(BmRuntime::apply, applicationExecutor);
    }

    private static List<BedrockPack> loadAll(ResourceManager manager) {
        List<BedrockPack> out = new ArrayList<>();
        Queries.setMissingHandler(name -> BedrockMotionClient.LOGGER.warn("Molang query not implemented: q.{}", name));
        Map<String, Object> config = BmConfig.read();
        BmConfig.enabled = config == null || Json.bool(config.get("enabled"), true);
        BmRuntime.replacePlayers = config != null && Json.bool(config.get("replacePlayers"), false);
        dev.bedrockmotion.bedrock.ParticleSystem.sizeFactor = config == null ? 1.0 : Math.max(0.05, Math.min(8, Json.num(config.get("particleScale"), 1.0)));
        Map<String, Object> features = config == null ? null : Json.obj(config.get("features"));
        BmRuntime.Features f = BmRuntime.features;
        f.entities = features == null || Json.bool(features.get("entities"), true);
        f.driver = features == null || Json.bool(features.get("driver"), true);
        f.attachables = features == null || Json.bool(features.get("attachables"), true);
        f.equipment = features == null || Json.bool(features.get("equipment"), true);
        f.particles = features == null || Json.bool(features.get("particles"), true);
        f.textureMeshes = features == null || Json.bool(features.get("textureMeshes"), true);
        f.sounds = features == null || Json.bool(features.get("sounds"), true);
        if (config != null && !Json.bool(config.get("enabled"), true)) return out;
        java.util.Set<String> basePacks = new java.util.HashSet<>();
        if (config != null && config.get("basePacks") instanceof List<?> bl) {
            for (Object o : bl) if (o instanceof String bs) basePacks.add(bs);
        }
        Path dir = FabricLoader.getInstance().getGameDir().resolve("bedrockmotion").resolve("packs");
        try {
            Files.createDirectories(dir);
            List<Path> entries;
            try (Stream<Path> s = Files.list(dir)) {
                entries = s.sorted().toList();
            }
            for (Path p : entries) {
                String name = p.getFileName().toString();
                String lower = name.toLowerCase(Locale.ROOT);
                if (!Files.isDirectory(p) && !lower.endsWith(".zip") && !lower.endsWith(".mcpack")) continue;
                try {
                    PackFiles files = PackFiles.open(p);
                    BedrockPack loadedPack = loadPack(name, files, config);
                    loadedPack.baseOnly = lower.startsWith("base_") || basePacks.contains(name);
                    if (loadedPack.baseOnly) {
                        BedrockMotionClient.LOGGER.info("Pack {} is a base pack: it only supplies animations, geometries and textures to the packs above it", name);
                    }
                    out.add(loadedPack);
                } catch (IOException | RuntimeException e) {
                    BedrockMotionClient.LOGGER.error("Pack {} could not be loaded", name, e);
                }
            }
        } catch (IOException e) {
            BedrockMotionClient.LOGGER.error("Pack folder not readable", e);
        }
        ResourceManagerFiles rm = new ResourceManagerFiles(manager);
        if (!rm.isEmpty()) {
            BedrockPack fromResources = loadPack("resourcepacks", rm, config);
            fromResources.javaResourcePack = true;
            out.add(fromResources);
        }
        out.sort((a, b) -> Boolean.compare(b.baseOnly, a.baseOnly));
        return out;
    }

    private static BedrockPack loadPack(String name, PackFiles files, Map<String, Object> config) {
        List<String> subs = BedrockPack.subpacks(files);
        String sub = subs.isEmpty() ? null : subs.get(subs.size() - 1);
        Map<String, Object> chosen = config == null ? null : Json.obj(config.get("subpacks"));
        if (chosen != null && chosen.get(name) instanceof String s) sub = s;
        long t0 = System.currentTimeMillis();
        BedrockPack pack = BedrockPack.load(name, files, sub);
        Map<String, Object> allSettings = config == null ? null : Json.obj(config.get("settings"));
        Map<String, Object> mine = allSettings == null ? null : Json.obj(allSettings.get(name));
        if (mine != null) {
            for (Map.Entry<String, Object> e : mine.entrySet()) {
                if (e.getValue() instanceof Boolean b) pack.settings.overrideToggle(e.getKey(), b);
                else if (e.getValue() instanceof String s) pack.settings.override(e.getKey(), s);
            }
        }
        BedrockMotionClient.LOGGER.info(
                "Pack {} (Subpack {}): {} in {} ms", name, sub, pack.summary(), System.currentTimeMillis() - t0);
        int shown = 0;
        for (String problem : pack.problems) {
            if (shown++ < 10) BedrockMotionClient.LOGGER.warn("  {}", problem);
        }
        return pack;
    }
}
