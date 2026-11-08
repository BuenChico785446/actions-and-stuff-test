package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.bedrock.Pose;
import java.util.ArrayList;
import java.util.List;

public final class BmModelPose {

    public final String[] names;
    public final float[][] values;

    private BmModelPose(String[] names, float[][] values) {
        this.names = names;
        this.values = values;
    }

    public static BmModelPose capture(Pose pose) {
        List<String> names = new ArrayList<>();
        List<float[]> values = new ArrayList<>();
        pose.forEachTouched((name, xf) -> {
            names.add(name);
            values.add(new float[] {
                (float) -xf.rx, (float) -xf.ry, (float) xf.rz,
                (float) -xf.px, (float) -xf.py, (float) xf.pz,
                (float) xf.sx, (float) xf.sy, (float) xf.sz
            });
        });
        if (names.isEmpty()) return null;
        return new BmModelPose(names.toArray(new String[0]), values.toArray(new float[0][]));
    }
}
