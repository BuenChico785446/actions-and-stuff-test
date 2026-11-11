package dev.bedrockmotion.client.runtime;

import com.mojang.blaze3d.platform.NativeImage;
import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.ParticleDef;
import dev.bedrockmotion.client.BedrockMotionClient;
import dev.bedrockmotion.pack.PngEncoder;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class BmTextures {

    private static final Identifier FALLBACK = Identifier.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    private static final Identifier BLOCK_ATLAS = Identifier.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    private static final Map<String, Identifier> CACHE = new HashMap<>();
    private static int counter;

    private BmTextures() {}

    public static boolean isFallback(Identifier id) {
        return id == FALLBACK;
    }

    public static void clear() {
        for (Identifier id : CACHE.values()) {
            if (id != FALLBACK) Minecraft.getInstance().getTextureManager().release(id);
        }
        CACHE.clear();
    }

    public static Identifier get(BedrockPack pack, String path) {
        if (path == null) return FALLBACK;
        if (ParticleDef.ATLAS_TERRAIN.equalsIgnoreCase(path)) return BLOCK_ATLAS;
        String key = pack.name + "|" + path.toLowerCase(Locale.ROOT);
        Identifier cached = CACHE.get(key);
        if (cached != null) return cached;
        Identifier id = load(pack, path);
        CACHE.put(key, id);
        return id;
    }

    private static Identifier load(BedrockPack pack, String path) {
        try {
            byte[] raw = pack.textureBytes(path);
            if (raw == null) {
                BedrockMotionClient.LOGGER.warn("Texture missing: {} (pack {})", path, pack.name);
                return FALLBACK;
            }
            byte[] png = PngEncoder.fromTexture(raw);
            NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
            Identifier id = Identifier.fromNamespaceAndPath(BedrockMotionClient.MOD_ID, "dyn/" + (counter++));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(null, image));
            return id;
        } catch (Exception e) {
            BedrockMotionClient.LOGGER.warn("Texture {} could not be loaded: {}", path, e.toString());
            return FALLBACK;
        }
    }
}
