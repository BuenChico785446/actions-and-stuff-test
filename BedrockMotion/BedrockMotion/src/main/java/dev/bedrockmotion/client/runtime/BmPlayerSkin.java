package dev.bedrockmotion.client.runtime;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

final class BmPlayerSkin {

    private BmPlayerSkin() {}

    static Identifier resolve(Entity entity) {
        Object skin = BmReflect.call(entity, "getSkin");
        if (skin == null) return null;
        for (String step : new String[] {"body", "texture"}) {
            Object next = BmReflect.call(skin, step);
            if (next instanceof Identifier id) return id;
            if (next != null) {
                for (String leaf : new String[] {"texturePath", "texture", "id"}) {
                    Object leafValue = BmReflect.call(next, leaf);
                    if (leafValue instanceof Identifier id) return id;
                }
            }
        }
        return null;
    }
}
