package dev.bedrockmotion.client.mixin;

import dev.bedrockmotion.client.runtime.BmAttachables;
import dev.bedrockmotion.client.runtime.BmItemCarrier;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {

    @Inject(
            method = "appendItemLayers(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V",
            at = @At("HEAD"),
            require = 0)
    private void bedrockmotion$recordItem(
            ItemStackRenderState output,
            ItemStack stack,
            ItemDisplayContext displayContext,
            Level level,
            ItemOwner owner,
            int seed,
            CallbackInfo ci) {
        String id = null;
        if (BmAttachables.available() && !stack.isEmpty()) id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        ((BmItemCarrier) (Object) output).bm$setItem(id, owner instanceof LivingEntity living ? living.getId() : -1);
    }
}
