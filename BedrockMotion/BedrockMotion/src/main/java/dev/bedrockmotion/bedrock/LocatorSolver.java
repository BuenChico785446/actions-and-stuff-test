package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.molang.Struct;

public final class LocatorSolver {

    private LocatorSolver() {}

    public static final class Solved {
        public final double[][] mats;
        public final double[][] rots;

        Solved(double[][] mats, double[][] rots) {
            this.mats = mats;
            this.rots = rots;
        }
    }

    public static Solved solveAll(GeometryDef geo, Pose pose) {
        int n = geo.bones.length;
        double[][] mats = new double[n][];
        double[][] rots = new double[n][];
        for (int i = 0; i < n; i++) {
            GeometryDef.Bone b = geo.bones[i];
            double[] m = b.parentIndex >= 0 ? mats[b.parentIndex].clone() : identity();
            double[] parentRot = b.parentIndex >= 0 ? rots[b.parentIndex] : identity3();
            Pose.Xf xf = pose.find(b.key);
            double sx = 1, sy = 1, sz = 1;
            double ax = 0, ay = 0, az = 0;
            if (xf != null) {
                translate(m, xf.px / 16.0, xf.py / 16.0, xf.pz / 16.0);
                ax = xf.rx;
                ay = xf.ry;
                az = xf.rz;
                sx = xf.sx;
                sy = xf.sy;
                sz = xf.sz;
            }
            double px = b.pivotX / 16.0, py = b.pivotY / 16.0, pz = b.pivotZ / 16.0;
            translate(m, px, py, pz);
            double[] local;
            if (xf != null && xf.entityRotation && b.parentIndex >= 0) {
                local = mul3(transpose3(parentRot), mul3(rotZyx(az, ay, ax), mul3(parentRot, rotZyx(b.rotZ, b.rotY, b.rotX))));
            } else {
                local = rotZyx(b.rotZ + az, b.rotY + ay, b.rotX + ax);
            }
            mulRot(m, local);
            scale(m, sx, sy, sz);
            translate(m, -px, -py, -pz);
            mats[i] = m;
            rots[i] = mul3(parentRot, local);
        }
        return new Solved(mats, rots);
    }

    public static double[] position(GeometryDef geo, Pose pose, String locatorName) {
        int[] bone = new int[1];
        GeometryDef.Locator loc = geo.findLocator(locatorName, bone);
        if (loc == null) return null;
        double[] m = solveAll(geo, pose).mats[bone[0]];
        return apply(m, loc.x / 16.0, loc.y / 16.0, loc.z / 16.0);
    }

    public static double[] bonePivot(GeometryDef geo, Pose pose, String boneName) {
        int i = geo.indexOf(boneName);
        if (i < 0) return null;
        GeometryDef.Bone b = geo.bones[i];
        return apply(solveAll(geo, pose).mats[i], b.pivotX / 16.0, b.pivotY / 16.0, b.pivotZ / 16.0);
    }

    public static Struct originStruct(GeometryDef geo, Pose pose, String boneName) {
        double[] p = bonePivot(geo, pose, boneName);
        Struct s = new Struct();
        s.fields.put("x", p == null ? 0.0 : -p[0] * 16.0);
        s.fields.put("y", p == null ? 0.0 : p[1] * 16.0);
        s.fields.put("z", p == null ? 0.0 : p[2] * 16.0);
        return s;
    }

    public static Struct trsStruct(Pose pose, double[] origin, String boneName) {
        Pose.Xf xf = pose.find(boneName.toLowerCase(java.util.Locale.ROOT));
        Struct t = vec(origin == null ? 0 : -origin[0] * 16.0, origin == null ? 0 : origin[1] * 16.0, origin == null ? 0 : origin[2] * 16.0);
        Struct r = xf == null ? vec(0, 0, 0) : vec(-Math.toDegrees(xf.rx), -Math.toDegrees(xf.ry), Math.toDegrees(xf.rz));
        Struct s = xf == null ? vec(1, 1, 1) : vec(xf.sx, xf.sy, xf.sz);
        Struct out = new Struct();
        out.fields.put("t", t);
        out.fields.put("r", r);
        out.fields.put("s", s);
        return out;
    }

    private static Struct vec(double x, double y, double z) {
        Struct s = new Struct();
        s.fields.put("x", x);
        s.fields.put("y", y);
        s.fields.put("z", z);
        return s;
    }

    private static double[] apply(double[] m, double x, double y, double z) {
        return new double[] {
            m[0] * x + m[4] * y + m[8] * z + m[12],
            m[1] * x + m[5] * y + m[9] * z + m[13],
            m[2] * x + m[6] * y + m[10] * z + m[14]
        };
    }

    private static double[] identity() {
        return new double[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    private static double[] identity3() {
        return new double[] {1, 0, 0, 0, 1, 0, 0, 0, 1};
    }

    private static double[] rotZyx(double rz, double ry, double rx) {
        double cz = Math.cos(rz), sz = Math.sin(rz);
        double cy = Math.cos(ry), sy = Math.sin(ry);
        double cx = Math.cos(rx), sx = Math.sin(rx);
        double[] z = {cz, -sz, 0, sz, cz, 0, 0, 0, 1};
        double[] y = {cy, 0, sy, 0, 1, 0, -sy, 0, cy};
        double[] x = {1, 0, 0, 0, cx, -sx, 0, sx, cx};
        return mul3(z, mul3(y, x));
    }

    private static double[] mul3(double[] a, double[] b) {
        double[] r = new double[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
            }
        }
        return r;
    }

    private static double[] transpose3(double[] a) {
        return new double[] {a[0], a[3], a[6], a[1], a[4], a[7], a[2], a[5], a[8]};
    }

    private static void mulRot(double[] m, double[] r) {
        for (int row = 0; row < 4; row++) {
            double c0 = m[row], c1 = m[4 + row], c2 = m[8 + row];
            m[row] = c0 * r[0] + c1 * r[3] + c2 * r[6];
            m[4 + row] = c0 * r[1] + c1 * r[4] + c2 * r[7];
            m[8 + row] = c0 * r[2] + c1 * r[5] + c2 * r[8];
        }
    }

    private static void translate(double[] m, double x, double y, double z) {
        m[12] += m[0] * x + m[4] * y + m[8] * z;
        m[13] += m[1] * x + m[5] * y + m[9] * z;
        m[14] += m[2] * x + m[6] * y + m[10] * z;
    }

    private static void scale(double[] m, double x, double y, double z) {
        for (int i = 0; i < 4; i++) {
            m[i] *= x;
            m[4 + i] *= y;
            m[8 + i] *= z;
        }
    }
}
