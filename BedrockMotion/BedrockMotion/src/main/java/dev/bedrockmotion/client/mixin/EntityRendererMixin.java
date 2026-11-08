package dev.bedrockmotion.client.mixin;

import dev.bedrockmotion.client.runtime.BmRuntime;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void bedrockmotion$extract(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        BmRuntime.extract(entity, state, partialTick);
    }
}
