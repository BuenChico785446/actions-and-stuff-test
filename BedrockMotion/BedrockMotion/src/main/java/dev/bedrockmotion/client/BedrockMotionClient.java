package dev.bedrockmotion.client;

import dev.bedrockmotion.client.runtime.BmPackLoader;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
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
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal(MOD_ID).executes(context -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.execute(() -> BmConfigScreen.show(new BmConfigScreen(null)));
                    return Command.SINGLE_SUCCESS;
                })));
        LOGGER.info("BedrockMotion ready. Put Bedrock packs into .minecraft/bedrockmotion/packs/");
    }
}
