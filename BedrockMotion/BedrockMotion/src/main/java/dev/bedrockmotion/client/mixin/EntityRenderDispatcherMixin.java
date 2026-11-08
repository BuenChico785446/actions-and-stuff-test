package dev.bedrockmotion.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.bedrockmotion.client.render.BmFrame;
import dev.bedrockmotion.client.runtime.BmModelHook;
import dev.bedrockmotion.client.runtime.BmParticleWorld;
import dev.bedrockmotion.client.runtime.BmRuntime;
import dev.bedrockmotion.client.runtime.BmStateHolder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @WrapOperation(
            method = "submit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit"))
    private void bedrockmotion$submit(
            EntityRenderer<?, ?> renderer,
            EntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            Operation<Void> original) {
        BmStateHolder holder = (BmStateHolder) (Object) state;
        BmFrame frame = holder.bm$getFrame();
        if (frame != null && frame.replacesModel()) {
            if (BmModelHook.seen()) original.call(renderer, state, poseStack, collector, camera);
            frame.submit(state.lightCoords, poseStack, collector, true);
        } else {
            original.call(renderer, state, poseStack, collector, camera);
        }
        if (frame != null && frame.hasParticles()) frame.submitParticles(poseStack, collector, camera);
        double[] origin = holder.bm$getOrigin();
        if (origin != null && BmParticleWorld.shouldRender(BmRuntime.worldFrame())) {
            BmParticleWorld.submit(origin, poseStack, collector, camera);
        }
    }
}
