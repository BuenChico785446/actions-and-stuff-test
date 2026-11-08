package dev.bedrockmotion.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.bedrockmotion.client.runtime.BmAttachables;
import dev.bedrockmotion.client.runtime.BmItemCarrier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin implements BmItemCarrier {

    @Shadow
    ItemDisplayContext displayContext;

    @Unique
    private String bm$itemId;

    @Unique
    private int bm$holderId = -1;

    @Override
    public void bm$setItem(String itemId, int holderId) {
        this.bm$itemId = itemId;
        this.bm$holderId = holderId;
    }

    @Override
    public String bm$getItemId() {
        return bm$itemId;
    }

    @Override
    public int bm$getHolderId() {
        return bm$holderId;
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true, require = 0)
    private void bedrockmotion$submit(
            CallbackInfo ci,
            @Local(argsOnly = true) PoseStack poseStack,
            @Local(argsOnly = true) SubmitNodeCollector collector,
            @Local(argsOnly = true, ordinal = 0) int lightCoords) {
        if (bm$itemId == null) return;
        if (BmAttachables.submit(bm$itemId, bm$holderId, displayContext, poseStack, collector, lightCoords)) ci.cancel();
    }
}
