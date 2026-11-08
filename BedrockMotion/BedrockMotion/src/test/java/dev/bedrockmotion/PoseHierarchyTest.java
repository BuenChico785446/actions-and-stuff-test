package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.GeometryDef;
import dev.bedrockmotion.bedrock.LocatorSolver;
import dev.bedrockmotion.bedrock.Pose;
import dev.bedrockmotion.bedrock.PoseHierarchy;
import dev.bedrockmotion.json.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class PoseHierarchyTest {

    private static final String GEOMETRY = """
        {"format_version":"1.12.0","minecraft:geometry":[{"description":{"identifier":"geometry.test","texture_width":64,"texture_height":64},
        "bones":[
        {"name":"waist","pivot":[0,12,0]},
        {"name":"body","parent":"waist","pivot":[0,24,0]},
        {"name":"head","parent":"body","pivot":[0,24,0]},
        {"name":"rightArm","parent":"body","pivot":[-5,22,0]},
        {"name":"hat","parent":"head","pivot":[0,24,0]}]}]}
        """;

    public static void main(String[] args) {
        List<GeometryDef> parsed = GeometryDef.parse(Json.parse(GEOMETRY), new ArrayList<>());
        GeometryDef geo = parsed.get(0);
        PoseHierarchy hierarchy = new PoseHierarchy(geo);
        Random random = new Random(7);
        double worst = 0;
        int checks = 0;
        for (int round = 0; round < 200; round++) {
            Pose pose = new Pose();
            for (String bone : new String[] {"waist", "body", "head", "rightArm"}) {
                Pose.Xf xf = pose.get(bone);
                xf.rx = Math.toRadians(random.nextDouble() * 120 - 60);
                xf.ry = Math.toRadians(random.nextDouble() * 120 - 60);
                xf.rz = Math.toRadians(random.nextDouble() * 120 - 60);
                xf.px = random.nextDouble() * 6 - 3;
                xf.py = random.nextDouble() * 6 - 3;
                xf.pz = random.nextDouble() * 6 - 3;
            }
            double[][] solved = LocatorSolver.solveAll(geo, pose).mats;
            double[][] ancestors = hierarchy.solve(pose);
            for (String bone : new String[] {"head", "rightArm", "hat", "body"}) {
                int i = geo.indexOf(bone);
                GeometryDef.Bone b = geo.bones[i];
                Pose.Xf own = pose.find(bone.toLowerCase());
                double[] pivotJava = {-b.pivotX, 24 - b.pivotY, b.pivotZ};
                float[] state = {
                    (float) pivotJava[0], (float) pivotJava[1], (float) pivotJava[2], 0, 0, 0, 1, 1, 1
                };
                if (own != null) {
                    state[0] += (float) -own.px;
                    state[1] += (float) -own.py;
                    state[2] += (float) own.pz;
                    state[3] += (float) -own.rx;
                    state[4] += (float) -own.ry;
                    state[5] += (float) own.rz;
                }
                if (ancestors[i] != null) PoseHierarchy.apply(ancestors[i], state);
                double[][] probes = {{0, 0, 0}, {3, -2, 5}, {-4, 6, 1}};
                for (double[] probe : probes) {
                    double[] frameRest = {b.pivotX / 16.0 + probe[0] / 16.0, b.pivotY / 16.0 + probe[1] / 16.0, b.pivotZ / 16.0 + probe[2] / 16.0};
                    double[] posed = apply(solved[i], frameRest);
                    double[] expected = {-posed[0] * 16, 24 - posed[1] * 16, posed[2] * 16};
                    double[] local = {-(probe[0]), -(probe[1]), probe[2]};
                    double[] actual = javaPoint(state, local);
                    for (int k = 0; k < 3; k++) worst = Math.max(worst, Math.abs(expected[k] - actual[k]));
                    checks++;
                }
            }
        }
        Pose waistOnly = new Pose();
        waistOnly.get("waist").rx = 0.5;
        dev.bedrockmotion.client.runtime.BmModelPose captured = dev.bedrockmotion.client.runtime.BmModelPose.capture(waistOnly, hierarchy);
        System.out.println("waist-only pose drives: " + String.join(",", captured.names));
        if (captured.names.length != 5) {
            System.out.println("hierarchy FAILED");
            System.exit(1);
        }
        System.out.printf("%d checks, worst deviation %.5f pixels%n", checks, worst);
        System.out.println(worst < 0.01 ? "hierarchy OK" : "hierarchy FAILED");
        if (worst >= 0.01) System.exit(1);
    }

    private static double[] apply(double[] m, double[] p) {
        return new double[] {
            m[0] * p[0] + m[4] * p[1] + m[8] * p[2] + m[12],
            m[1] * p[0] + m[5] * p[1] + m[9] * p[2] + m[13],
            m[2] * p[0] + m[6] * p[1] + m[10] * p[2] + m[14]
        };
    }

    private static double[] javaPoint(float[] s, double[] local) {
        double x = local[0] * s[6], y = local[1] * s[7], z = local[2] * s[8];
        double cz = Math.cos(s[5]), sz = Math.sin(s[5]);
        double cy = Math.cos(s[4]), sy = Math.sin(s[4]);
        double cx = Math.cos(s[3]), sx = Math.sin(s[3]);
        double ax = x, ay = y * cx - z * sx, az = y * sx + z * cx;
        double bx = ax * cy + az * sy, by = ay, bz = -ax * sy + az * cy;
        double rx = bx * cz - by * sz, ry = bx * sz + by * cz, rz = bz;
        return new double[] {rx + s[0], ry + s[1], rz + s[2]};
    }
}
