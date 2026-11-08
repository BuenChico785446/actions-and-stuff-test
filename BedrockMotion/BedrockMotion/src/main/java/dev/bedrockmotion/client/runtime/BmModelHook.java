package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.bedrock.PoseHierarchy;
import dev.bedrockmotion.client.BedrockMotionClient;
import dev.bedrockmotion.client.mixin.ModelPartAccessor;
import dev.bedrockmotion.client.render.BmFrame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;

public final class BmModelHook {

    private static final Set<ModelPart> HIDDEN = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<ModelPart, Index> INDEX = new WeakHashMap<>();
    private static final Map<String, String> ALIASES = Map.of(
            "leg0", "righthindleg",
            "leg1", "lefthindleg",
            "leg2", "rightfrontleg",
            "leg3", "leftfrontleg");
    private static final float[] STATE = new float[9];
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

    private record Index(Map<String, ModelPart> parts, Map<ModelPart, Set<String>> javaAncestors) {}

    private static void drive(ModelPart root, BmModelPose pose) {
        Index index = INDEX.computeIfAbsent(root, BmModelHook::buildIndex);
        for (int i = 0; i < pose.names.length; i++) {
            String key = PoseHierarchy.normalize(pose.names[i]);
            ModelPart part = index.parts().get(key);
            if (part == null) {
                String alias = ALIASES.get(key);
                if (alias != null) part = index.parts().get(alias);
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
            double[] ancestor = pose.ancestors[i];
            if (ancestor != null && !nestedInJava(index, part, pose.chains[i])) applyAncestors(part, ancestor);
        }
    }

    private static boolean nestedInJava(Index index, ModelPart part, String[] bedrockChain) {
        Set<String> javaAncestors = index.javaAncestors().get(part);
        if (javaAncestors == null || javaAncestors.isEmpty()) return false;
        for (String name : bedrockChain) {
            if (javaAncestors.contains(name)) return true;
        }
        return false;
    }

    private static void applyAncestors(ModelPart part, double[] ancestor) {
        float[] s = STATE;
        s[0] = part.x;
        s[1] = part.y;
        s[2] = part.z;
        s[3] = part.xRot;
        s[4] = part.yRot;
        s[5] = part.zRot;
        s[6] = part.xScale;
        s[7] = part.yScale;
        s[8] = part.zScale;
        PoseHierarchy.apply(ancestor, s);
        part.x = s[0];
        part.y = s[1];
        part.z = s[2];
        part.xRot = s[3];
        part.yRot = s[4];
        part.zRot = s[5];
        part.xScale = s[6];
        part.yScale = s[7];
        part.zScale = s[8];
    }

    private static Index buildIndex(ModelPart root) {
        Map<String, ModelPart> parts = new HashMap<>();
        Map<ModelPart, Set<String>> ancestors = new IdentityHashMap<>();
        collect(root, parts, ancestors, new ArrayList<>());
        return new Index(parts, ancestors);
    }

    private static void collect(ModelPart part, Map<String, ModelPart> parts, Map<ModelPart, Set<String>> ancestors, List<String> path) {
        if (path.size() > 32) return;
        for (Map.Entry<String, ModelPart> e : ((ModelPartAccessor) (Object) part).bedrockmotion$children().entrySet()) {
            String name = PoseHierarchy.normalize(e.getKey());
            parts.putIfAbsent(name, e.getValue());
            ancestors.put(e.getValue(), new HashSet<>(path));
            path.add(name);
            collect(e.getValue(), parts, ancestors, path);
            path.remove(path.size() - 1);
        }
    }
}
