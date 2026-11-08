package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.client.BedrockMotionClient;
import dev.bedrockmotion.bedrock.ParticleSystem;
import dev.bedrockmotion.client.render.BmParticleRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;

public final class BmParticleWorld {

    private record Orphan(ParticleSystem system, BedrockPack pack) {}

    private static final List<Orphan> ORPHANS = new ArrayList<>();
    private static final int MAX_ORPHANS = 48;
    private static long lastRendered = -1;

    private BmParticleWorld() {}

    static void adopt(ParticleSystem system, BedrockPack pack) {
        system.detach();
        system.pendingSounds.clear();
        BedrockMotionClient.once("orphan", "BedrockMotion: particles of removed entities keep running until they expire");
        ORPHANS.add(new Orphan(system, pack));
        while (ORPHANS.size() > MAX_ORPHANS) ORPHANS.remove(0);
    }

    static void clear() {
        ORPHANS.clear();
    }

    static void update(double dt) {
        if (ORPHANS.isEmpty()) return;
        for (Orphan o : ORPHANS) {
            o.system().updateDetached(dt);
            o.system().pendingSounds.clear();
        }
        ORPHANS.removeIf(o -> o.system().isEmpty());
    }

    public static boolean shouldRender(long frame) {
        if (ORPHANS.isEmpty() || frame == lastRendered) return false;
        lastRendered = frame;
        return true;
    }

    public static void submit(double[] origin, PoseStack poseStack, SubmitNodeCollector collector, Object camera) {
        List<ParticleSystem.EmitterView> views = new ArrayList<>();
        List<Identifier> textures = new ArrayList<>();
        for (Orphan o : ORPHANS) {
            List<ParticleSystem.EmitterView> own = new ArrayList<>();
            o.system().collectWorld(origin, own);
            for (ParticleSystem.EmitterView v : own) {
                views.add(v);
                textures.add(BmTextures.get(o.pack(), v.def.texture));
            }
        }
        if (views.isEmpty()) return;
        BmParticleRenderer.submit(views, textures.toArray(new Identifier[0]), poseStack, collector, camera);
    }
}
