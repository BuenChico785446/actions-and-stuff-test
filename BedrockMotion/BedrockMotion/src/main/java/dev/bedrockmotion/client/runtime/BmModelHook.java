package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.client.BedrockMotionClient;
import dev.bedrockmotion.client.mixin.ModelPartAccessor;
import dev.bedrockmotion.client.render.BmFrame;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;

public final class BmModelHook {

    private static final Set<ModelPart> HIDDEN = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<ModelPart, Map<String, ModelPart>> INDEX = new WeakHashMap<>();
    private static final Map<String, String> ALIASES = Map.of(
            "leg0", "righthindleg",
            "leg1", "lefthindleg",
            "leg2", "rightfrontleg",
            "leg3", "leftfrontleg");
    private static volatile boolean seen;
    private static boolean failed;

    private BmModelHook() {}

    public static boolean seen() {
        return seen;
    }

    public static void apply(Model model, Object state) {
        if (!(state instanceof BmStateHolder holder)) return;
        seen = true;
        BmFrame frame = holder.bm$getFrame();
        ModelPart root = model.root();
        if (frame != null && frame.replacesModel()) {
            root.visible = false;
            HIDDEN.add(root);
            return;
        }
        if (HIDDEN.remove(root)) root.visible = true;
        if (frame == null || frame.driver() == null || failed) return;
        try {
            drive(root, frame.driver());
        } catch (RuntimeException | LinkageError e) {
            failed = true;
            BedrockMotionClient.LOGGER.error("BedrockMotion: applying poses to vanilla models failed", e);
        }
    }

    private static void drive(ModelPart root, BmModelPose pose) {
        Map<String, ModelPart> index = INDEX.computeIfAbsent(root, BmModelHook::buildIndex);
        for (int i = 0; i < pose.names.length; i++) {
            String key = normalize(pose.names[i]);
            ModelPart part = index.get(key);
            if (part == null) {
                String alias = ALIASES.get(key);
                if (alias != null) part = index.get(alias);
            }
            if (part == null) continue;
            float[] v = pose.values[i];
            part.resetPose();
            part.xRot += v[0];
            part.yRot += v[1];
            part.zRot += v[2];
            part.x += v[3];
            part.y += v[4];
            part.z += v[5];
            part.xScale *= v[6];
            part.yScale *= v[7];
            part.zScale *= v[8];
        }
    }

    private static Map<String, ModelPart> buildIndex(ModelPart root) {
        Map<String, ModelPart> out = new HashMap<>();
        collect(root, out, 0);
        return out;
    }

    private static void collect(ModelPart part, Map<String, ModelPart> out, int depth) {
        if (depth > 32) return;
        for (Map.Entry<String, ModelPart> e : ((ModelPartAccessor) (Object) part).bedrockmotion$children().entrySet()) {
            out.putIfAbsent(normalize(e.getKey()), e.getValue());
            collect(e.getValue(), out, depth + 1);
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }
}
