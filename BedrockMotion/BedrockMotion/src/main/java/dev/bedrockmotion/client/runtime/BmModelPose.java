package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.bedrock.Pose;
import dev.bedrockmotion.bedrock.PoseHierarchy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BmModelPose {

    private static final float[] IDENTITY = {0, 0, 0, 0, 0, 0, 1, 1, 1};

    public final String[] names;
    public final float[][] values;
    public final double[][] ancestors;
    public final String[][] chains;

    private BmModelPose(String[] names, float[][] values, double[][] ancestors, String[][] chains) {
        this.names = names;
        this.values = values;
        this.ancestors = ancestors;
        this.chains = chains;
    }

    public static BmModelPose capture(Pose pose, PoseHierarchy hierarchy) {
        double[][] solved = hierarchy == null ? null : hierarchy.solve(pose);
        List<String> names = new ArrayList<>();
        List<float[]> values = new ArrayList<>();
        List<double[]> ancestors = new ArrayList<>();
        List<String[]> chains = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        pose.forEachTouched((name, xf) -> {
            names.add(name);
            seen.add(PoseHierarchy.normalize(name));
            values.add(new float[] {
                (float) -xf.rx, (float) -xf.ry, (float) xf.rz,
                (float) -xf.px, (float) -xf.py, (float) xf.pz,
                (float) xf.sx, (float) xf.sy, (float) xf.sz
            });
            int bone = hierarchy == null ? -1 : hierarchy.indexOf(name);
            ancestors.add(bone < 0 ? null : solved[bone]);
            chains.add(bone < 0 ? null : hierarchy.ancestors(bone));
        });
        if (solved != null) {
            for (int bone = 0; bone < solved.length; bone++) {
                if (solved[bone] == null || seen.contains(hierarchy.key(bone))) continue;
                names.add(hierarchy.name(bone));
                values.add(IDENTITY.clone());
                ancestors.add(solved[bone]);
                chains.add(hierarchy.ancestors(bone));
            }
        }
        if (names.isEmpty()) return null;
        return new BmModelPose(
                names.toArray(new String[0]),
                values.toArray(new float[0][]),
                ancestors.toArray(new double[0][]),
                chains.toArray(new String[0][]));
    }
}
