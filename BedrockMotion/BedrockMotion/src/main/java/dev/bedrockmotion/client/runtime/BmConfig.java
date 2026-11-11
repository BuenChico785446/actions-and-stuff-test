package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.bedrock.ParticleSystem;
import dev.bedrockmotion.client.config.BedrockMotionConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

public final class BmConfig {

    private static volatile ConfigHolder<BedrockMotionConfig> holder;
    private static volatile String loadedSignature;

    private BmConfig() {}

    public static void init() {
        ConfigHolder<BedrockMotionConfig> registered = AutoConfig.register(BedrockMotionConfig.class, GsonConfigSerializer::new);
        registered.registerSaveListener((manager, config) -> {
            apply(config);
            if (!config.packSignature().equals(loadedSignature)) {
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.execute(() -> minecraft.reloadResourcePacks());
            }
            return InteractionResult.SUCCESS;
        });
        holder = registered;
        apply(registered.getConfig());
    }

    public static BedrockMotionConfig get() {
        ConfigHolder<BedrockMotionConfig> current = holder;
        return current == null ? new BedrockMotionConfig() : current.getConfig();
    }

    static void markLoaded(BedrockMotionConfig config) {
        loadedSignature = config.packSignature();
    }

    static void apply(BedrockMotionConfig config) {
        BmRuntime.replacePlayers = config.replacePlayers;
        ParticleSystem.sizeFactor = config.clampedParticleScale();
        BedrockMotionConfig.Features chosen = config.features == null ? new BedrockMotionConfig.Features() : config.features;
        BmRuntime.Features f = BmRuntime.features;
        f.entities = chosen.entities;
        f.driver = chosen.driver;
        f.attachables = chosen.attachables;
        f.equipment = chosen.equipment;
        f.particles = chosen.particles;
        f.textureMeshes = chosen.textureMeshes;
        f.sounds = chosen.sounds;
    }
}
