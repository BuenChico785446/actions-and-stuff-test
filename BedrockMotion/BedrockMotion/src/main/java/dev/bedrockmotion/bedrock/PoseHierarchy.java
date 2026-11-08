package dev.bedrockmotion.bedrock;

import java.util.Locale;

/** Java model space is the render frame turned 180 degrees about z and shifted down by 24 pixels. */
public final class PoseHierarchy {

    public static final int LINEAR = 9;
    public static final int SIZE = 12;
    private static final double EPS = 1.0e-9;
    private static final double[] JAVA_ORIGIN = {0, 24, 0};
    private static final double[] FLIP = {-1, -1, 1};

    private final GeometryDef geometry;
    private final String[] keys;
    private final String[][] chains;
    private final double[][] restInverse;

    public PoseHierarchy(GeometryDef geometry) {
        this.geometry = geometry;
        int n = geometry.bones.length;
        keys = new String[n];
        chains = new String[n][];
        for (int i = 0; i < n; i++) {
            keys[i] = normalize(geometry.bones[i].name);
            int depth = 0;
            for (int p = geometry.bones[i].parentIndex; p >= 0 && depth < n; p = geometry.bones[p].parentIndex) depth++;
            chains[i] = new String[depth];
            int at = 0;
            for (int p = geometry.bones[i].parentIndex; p >= 0 && at < depth; p = geometry.bones[p].parentIndex) {
                chains[i][at++] = normalize(geometry.bones[p].name);
            }
        }
        double[][] rest = LocatorSolver.solveAll(geometry, new Pose()).mats;
        restInverse = new double[n][];
        for (int i = 0; i < n; i++) restInverse[i] = inverseAffine(rest[i]);
    }

    public static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }

    public int boneCount() {
        return keys.length;
    }

    public String key(int bone) {
        return keys[bone];
    }

    public String name(int bone) {
        return geometry.bones[bone].key;
    }

    public int indexOf(String bone) {
        return geometry.indexOf(bone);
    }

    public String[] ancestors(int bone) {
        return chains[bone];
    }

    /** Per bone: 9 row-major linear entries then the translation in pixels, or null when no parent is posed. */
    public double[][] solve(Pose pose) {
        int n = keys.length;
        double[][] mats = LocatorSolver.solveAll(geometry, pose).mats;
        double[][] out = new double[n][];
        for (int i = 0; i < n; i++) {
            int p = geometry.bones[i].parentIndex;
            if (p < 0) continue;
            double[] delta = multiply(mats[p], restInverse[p]);
            if (isIdentity(delta)) continue;
            out[i] = toJava(delta);
        }
        return out;
    }

    /** state: x, y, z, xRot, yRot, zRot, xScale, yScale, zScale of a Java part that already has its own pose. */
    public static void apply(double[] ancestor, float[] state) {
        double dx = state[0] - JAVA_ORIGIN[0];
        double dy = state[1] - JAVA_ORIGIN[1];
        double dz = state[2] - JAVA_ORIGIN[2];
        state[0] = (float) (ancestor[0] * dx + ancestor[1] * dy + ancestor[2] * dz + ancestor[LINEAR] + JAVA_ORIGIN[0]);
        state[1] = (float) (ancestor[3] * dx + ancestor[4] * dy + ancestor[5] * dz + ancestor[LINEAR + 1] + JAVA_ORIGIN[1]);
        state[2] = (float) (ancestor[6] * dx + ancestor[7] * dy + ancestor[8] * dz + ancestor[LINEAR + 2] + JAVA_ORIGIN[2]);
        double[] rotation = orthonormal(ancestor);
        double scale = Math.cbrt(Math.abs(determinant(ancestor)));
        double[] own = rotationZyx(state[5], state[4], state[3]);
        double[] combined = multiply3(rotation, own);
        double sy = -combined[6];
        double cy = Math.sqrt(combined[0] * combined[0] + combined[3] * combined[3]);
        double xr, yr, zr;
        if (cy > 1.0e-6) {
            xr = Math.atan2(combined[7], combined[8]);
            yr = Math.atan2(sy, cy);
            zr = Math.atan2(combined[3], combined[0]);
        } else {
            xr = Math.atan2(-combined[5], combined[4]);
            yr = Math.atan2(sy, cy);
            zr = 0;
        }
        state[3] = (float) xr;
        state[4] = (float) yr;
        state[5] = (float) zr;
        if (scale > EPS) {
            state[6] *= (float) scale;
            state[7] *= (float) scale;
            state[8] *= (float) scale;
        }
    }

    private static double[] toJava(double[] m) {
        double[] out = new double[SIZE];
        double[] c = FLIP;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                out[row * 3 + col] = c[row] * c[col] * m[col * 4 + row];
            }
        }
        for (int row = 0; row < 3; row++) out[LINEAR + row] = c[row] * m[12 + row] * 16.0;
        return out;
    }

    private static boolean isIdentity(double[] m) {
        double[] id = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        for (int i = 0; i < 16; i++) {
            if (Math.abs(m[i] - id[i]) > 1.0e-7) return false;
        }
        return true;
    }

    private static double[] multiply(double[] a, double[] b) {
        double[] r = new double[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) sum += a[k * 4 + row] * b[col * 4 + k];
                r[col * 4 + row] = sum;
            }
        }
        return r;
    }

    private static double[] inverseAffine(double[] m) {
        double[] l = {m[0], m[4], m[8], m[1], m[5], m[9], m[2], m[6], m[10]};
        double[] inv = invert3(l);
        double[] r = new double[16];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) r[col * 4 + row] = inv[row * 3 + col];
        }
        for (int row = 0; row < 3; row++) {
            r[12 + row] = -(inv[row * 3] * m[12] + inv[row * 3 + 1] * m[13] + inv[row * 3 + 2] * m[14]);
        }
        r[15] = 1;
        return r;
    }

    private static double determinant(double[] m) {
        return m[0] * (m[4] * m[8] - m[5] * m[7]) - m[1] * (m[3] * m[8] - m[5] * m[6]) + m[2] * (m[3] * m[7] - m[4] * m[6]);
    }

    private static double[] invert3(double[] m) {
        double det = determinant(m);
        if (Math.abs(det) < EPS) return new double[] {1, 0, 0, 0, 1, 0, 0, 0, 1};
        double d = 1.0 / det;
        return new double[] {
            (m[4] * m[8] - m[5] * m[7]) * d, (m[2] * m[7] - m[1] * m[8]) * d, (m[1] * m[5] - m[2] * m[4]) * d,
            (m[5] * m[6] - m[3] * m[8]) * d, (m[0] * m[8] - m[2] * m[6]) * d, (m[2] * m[3] - m[0] * m[5]) * d,
            (m[3] * m[7] - m[4] * m[6]) * d, (m[1] * m[6] - m[0] * m[7]) * d, (m[0] * m[4] - m[1] * m[3]) * d
        };
    }

    private static double[] orthonormal(double[] m) {
        double[] c0 = {m[0], m[3], m[6]};
        double[] c1 = {m[1], m[4], m[7]};
        double[] c2 = {m[2], m[5], m[8]};
        normalize3(c0);
        double dot = c1[0] * c0[0] + c1[1] * c0[1] + c1[2] * c0[2];
        for (int i = 0; i < 3; i++) c1[i] -= dot * c0[i];
        normalize3(c1);
        c2[0] = c0[1] * c1[2] - c0[2] * c1[1];
        c2[1] = c0[2] * c1[0] - c0[0] * c1[2];
        c2[2] = c0[0] * c1[1] - c0[1] * c1[0];
        return new double[] {c0[0], c1[0], c2[0], c0[1], c1[1], c2[1], c0[2], c1[2], c2[2]};
    }

    private static void normalize3(double[] v) {
        double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len < EPS) return;
        v[0] /= len;
        v[1] /= len;
        v[2] /= len;
    }

    private static double[] rotationZyx(double rz, double ry, double rx) {
        double cz = Math.cos(rz), sz = Math.sin(rz);
        double cy = Math.cos(ry), sy = Math.sin(ry);
        double cx = Math.cos(rx), sx = Math.sin(rx);
        return new double[] {
            cy * cz, sx * sy * cz - cx * sz, cx * sy * cz + sx * sz,
            cy * sz, sx * sy * sz + cx * cz, cx * sy * sz - sx * cz,
            -sy, sx * cy, cx * cy
        };
    }

    private static double[] multiply3(double[] a, double[] b) {
        double[] r = new double[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
            }
        }
        return r;
    }
}
