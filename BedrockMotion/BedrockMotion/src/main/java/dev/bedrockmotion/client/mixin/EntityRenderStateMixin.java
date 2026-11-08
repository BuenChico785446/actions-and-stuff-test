package dev.bedrockmotion.client.mixin;

import dev.bedrockmotion.client.render.BmFrame;
import dev.bedrockmotion.client.runtime.BmStateHolder;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements BmStateHolder {

    @Unique
    private BmFrame bm$frame;

    @Unique
    private double[] bm$origin;

    @Override
    public double[] bm$getOrigin() {
        return bm$origin;
    }

    @Override
    public void bm$setOrigin(double[] origin) {
        this.bm$origin = origin;
    }

    @Override
    public BmFrame bm$getFrame() {
        return bm$frame;
    }

    @Override
    public void bm$setFrame(BmFrame frame) {
        this.bm$frame = frame;
    }
}
