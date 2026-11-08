package dev.bedrockmotion.client.runtime;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.RenderPlan;
import dev.bedrockmotion.bedrock.RenderPlanner;
import dev.bedrockmotion.client.BedrockMotionClient;
import dev.bedrockmotion.client.render.BmFrame;
import dev.bedrockmotion.molang.Env;
import dev.bedrockmotion.molang.EntitySnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;

public final class BmAttachables {

    private BmAttachables() {}

    private static void alignToThirdPersonHand(PoseStack poseStack, boolean left) {
        float side = left ? -1f : 1f;
        poseStack.translate(-side / 16f, -0.125f, 0.625f);
        poseStack.rotate(Axis.YP.rotationDegrees(-180f));
        poseStack.rotate(Axis.XP.rotationDegrees(90f));
        poseStack.translate(0f, 7f / 16f, 1f / 16f);
        poseStack.rotate(Axis.ZP.rotationDegrees(180f));
    }

    public static boolean available() {
        return BmRuntime.hasAttachables();
    }

    public static boolean submit(
            String itemId,
            int holderId,
            ItemDisplayContext context,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light) {
        ClientEntityDef def = BmRuntime.attachable(itemId);
        if (def == null || context == null) return false;
        String ctx = context.name();
        boolean firstPerson = ctx.startsWith("FIRST_PERSON");
        if (!firstPerson && !ctx.startsWith("THIRD_PERSON")) return false;
        if (!firstPerson && BmRuntime.ownerReplaced(holderId)) return true;
        Minecraft mc = Minecraft.getInstance();
        Entity holder = mc.level == null || holderId < 0 ? null : mc.level.getEntity(holderId);
        BmRuntime.Entry e = BmRuntime.attachmentEntry(holderId + "|" + itemId, def);
        if (e.failed) return false;
        try {
            long now = System.nanoTime();
            double dt = e.lastNanos == 0 ? 0 : Math.min(0.1, Math.max(0, (now - e.lastNanos) / 1e9));
            if (e.frame == null || dt >= 0.0005) {
                Env env = e.animator.env;
                EntitySnapshot snapshot = env.entity;
                snapshot.identifier = itemId;
                snapshot.itemName = itemId;
                snapshot.inUi = false;
                env.context.put("is_first_person", firstPerson ? 1.0 : 0.0);
                env.context.put("item_slot", "main_hand");
                if (holder != null) {
                    e.probe.entity = holder;
                    env.probe = e.probe;
                    BmRuntime.fill(holder, e, snapshot, 1.0f, dt);
                    env.context.put("owning_entity", BmRuntime.envOf(holderId));
                }
                snapshot.engineVars.put("is_first_person", firstPerson ? 1.0 : 0.0);
                double life = holder != null ? holder.tickCount / 20.0 : env.lifeTime + dt;
                e.animator.update(dt, life, 1.0);
                RenderPlan plan = RenderPlanner.plan(e.animator);
                e.frame = BmFrame.build(e.animator, plan, 0f, BmFrame.Context.NONE, null, java.util.List.of(), null);
                e.lastNanos = now;
                if (holder != null) BmRuntime.playSounds(holder, e.animator);
            }
            if (!e.frame.replacesModel()) return false;
            BedrockMotionClient.once("attachable:" + itemId, "BedrockMotion: drawing attachable for {}", itemId);
            poseStack.pushPose();
            if (!firstPerson) alignToThirdPersonHand(poseStack, ctx.contains("LEFT"));
            e.frame.submit(light, poseStack, collector, false);
            poseStack.popPose();
            return true;
        } catch (RuntimeException ex) {
            e.failed = true;
            BedrockMotionClient.LOGGER.error("BedrockMotion: attachable {} disabled", itemId, ex);
            return false;
        }
    }
}
