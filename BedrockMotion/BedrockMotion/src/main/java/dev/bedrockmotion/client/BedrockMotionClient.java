package dev.bedrockmotion.client;

import dev.bedrockmotion.client.runtime.BmPackLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BedrockMotionClient implements ClientModInitializer {

    public static final String MOD_ID = "bedrockmotion";
    public static final Logger LOGGER = LoggerFactory.getLogger("BedrockMotion");
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    public static void once(String key, String message, Object... args) {
        if (LOGGED.add(key)) LOGGER.info(message, args);
    }


    @Override
    public void onInitializeClient() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(Identifier.fromNamespaceAndPath(MOD_ID, "packs"), new BmPackLoader());
        LOGGER.info("BedrockMotion ready. Put Bedrock packs into .minecraft/bedrockmotion/packs/");
    }
}
