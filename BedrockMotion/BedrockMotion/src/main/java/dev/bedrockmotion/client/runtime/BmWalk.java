package dev.bedrockmotion.client.runtime;

import java.lang.reflect.Field;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Reflection keeps this compiling if the walkAnimation field changes between Minecraft versions. */
final class BmWalk {

    private static Field field;
    private static boolean searched;

    private BmWalk() {}

    static Object state(Entity entity) {
        if (!(entity instanceof LivingEntity)) return null;
        if (!searched) {
            searched = true;
            try {
                field = LivingEntity.class.getField("walkAnimation");
            } catch (ReflectiveOperationException | RuntimeException e) {
                field = null;
            }
        }
        if (field == null) return null;
        try {
            return field.get(entity);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
