package dev.bedrockmotion.bedrock;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

public final class Pose {

    public static final class Xf {
        public double rx, ry, rz;
        public double px, py, pz;
        public double sx = 1, sy = 1, sz = 1;
        public boolean touched;
        public boolean entityRotation;

        void reset() {
            rx = ry = rz = 0;
            px = py = pz = 0;
            sx = sy = sz = 1;
            touched = false;
            entityRotation = false;
        }

        public boolean isIdentity() {
            return rx == 0 && ry == 0 && rz == 0 && px == 0 && py == 0 && pz == 0 && sx == 1 && sy == 1 && sz == 1;
        }
    }

    private final Map<String, Xf> bones = new HashMap<>();
    private final Map<String, Xf> byGivenName = new HashMap<>();

    public Xf get(String bone) {
        Xf x = byGivenName.get(bone);
        if (x == null) {
            x = bones.computeIfAbsent(bone.toLowerCase(Locale.ROOT), k -> new Xf());
            byGivenName.put(bone, x);
        }
        x.touched = true;
        return x;
    }

    public Xf find(String lowerBone) {
        return bones.get(lowerBone);
    }

    public void reset() {
        for (Xf x : bones.values()) x.reset();
    }

    public void forEachTouched(BiConsumer<String, Xf> action) {
        for (Map.Entry<String, Xf> e : bones.entrySet()) {
            if (e.getValue().touched) action.accept(e.getKey(), e.getValue());
        }
    }

    public int touchedCount() {
        int count = 0;
        for (Xf xf : bones.values()) {
            if (xf.touched) count++;
        }
        return count;
    }

    public int size() {
        return bones.size();
    }
}
