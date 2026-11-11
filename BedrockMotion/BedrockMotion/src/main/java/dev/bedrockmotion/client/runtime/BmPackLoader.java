package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.client.BedrockMotionClient;
import dev.bedrockmotion.client.config.BedrockMotionConfig;
import dev.bedrockmotion.molang.Queries;
import dev.bedrockmotion.pack.PackFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
        BedrockMotionConfig config = BmConfig.get();
        BmConfig.apply(config);
        BmConfig.markLoaded(config);
        if (!config.enabled) return out;
        Set<String> basePacks = config.basePackNames();
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

    private static BedrockPack loadPack(String name, PackFiles files, BedrockMotionConfig config) {
        List<String> subs = BedrockPack.subpacks(files);
        String sub = subs.isEmpty() ? null : subs.get(subs.size() - 1);
        String chosen = config.subpackFor(name);
        if (chosen != null) sub = chosen;
        long t0 = System.currentTimeMillis();
        BedrockPack pack = BedrockPack.load(name, files, sub);
        for (Map.Entry<String, Object> e : config.settingsFor(name).entrySet()) {
            if (e.getValue() instanceof Boolean b) pack.settings.overrideToggle(e.getKey(), b);
            else if (e.getValue() instanceof String s) pack.settings.override(e.getKey(), s);
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
