package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.client.BedrockMotionClient;
import dev.bedrockmotion.pack.PathMapper;
import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

public final class BmSounds {

    private static Method createEvent;
    private static Method playLocal;
    private static Object source;
    private static Class<?> levelClass;
    private static boolean failed;
    private static boolean warnedCustom;

    private BmSounds() {}

    public static void play(Entity entity, BedrockPack pack, String event) {
        if (failed || !BmRuntime.features.sounds) return;
        Identifier id = resolve(pack, event);
        if (id == null) return;
        try {
            Object level = entity.level();
            if (playLocal == null || levelClass != level.getClass()) {
                Class<?> soundEvent = Class.forName("net.minecraft.sounds.SoundEvent");
                Class<?> soundSource = Class.forName("net.minecraft.sounds.SoundSource");
                createEvent = soundEvent.getMethod("createVariableRangeEvent", Identifier.class);
                source = soundSource.getField("NEUTRAL").get(null);
                playLocal = level.getClass().getMethod(
                        "playLocalSound",
                        double.class, double.class, double.class,
                        soundEvent, soundSource, float.class, float.class, boolean.class);
                levelClass = level.getClass();
            }
            Object sound = createEvent.invoke(null, id);
            playLocal.invoke(level, entity.getX(), entity.getY(), entity.getZ(), sound, source, 1.0f, 1.0f, false);
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            BedrockMotionClient.LOGGER.warn("BedrockMotion: sound playback disabled ({})", e.toString());
        }
    }

    static Identifier resolve(BedrockPack pack, String event) {
        if (pack.soundEvents.contains(event)) {
            if (pack.javaResourcePack) {
                return Identifier.fromNamespaceAndPath(BedrockMotionClient.MOD_ID, PathMapper.sanitize(event));
            }
            if (!warnedCustom) {
                warnedCustom = true;
                BedrockMotionClient.LOGGER.info(
                        "BedrockMotion: custom pack sounds only play from a converted resource pack (see PackConverter)");
            }
            return null;
        }
        String vanilla = vanilla(event.toLowerCase(Locale.ROOT));
        if (vanilla == null) return null;
        Identifier id = Identifier.fromNamespaceAndPath("minecraft", vanilla);
        return BuiltInRegistries.SOUND_EVENT.containsKey(id) ? id : null;
    }

    private static String vanilla(String event) {
        String e = event.startsWith("minecraft:") ? event.substring(10) : event;
        String[] parts = e.split("\\.");
        if (parts.length != 3 || !parts[0].equals("mob")) return null;
        String suffix = switch (parts[2]) {
            case "say", "idle" -> "ambient";
            case "hurt" -> "hurt";
            case "death" -> "death";
            case "step" -> "step";
            default -> null;
        };
        return suffix == null ? null : "entity." + parts[1] + "." + suffix;
    }
}
