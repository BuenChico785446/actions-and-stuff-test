package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.json.Json;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GeometryDef {

    public static final int NORTH = 0, SOUTH = 1, EAST = 2, WEST = 3, UP = 4, DOWN = 5;

    public static final class Quad {
        public final float[] pos = new float[12];
        public final float[] uv = new float[8];
        public final float[] normal = new float[3];
        public String materialInstance;
    }

    public static final class Cube {
        public final Quad[] quads = new Quad[6];
        public float pivotX, pivotY, pivotZ;
        public float rotX, rotY, rotZ;
        public boolean rotated;
    }

    public static final class Locator {
        public final String name;
        public final float x, y, z;

        Locator(String name, float x, float y, float z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static final class TextureMesh {
        public String texture;
        public float px, py, pz;
        public float rx, ry, rz;
        public float sx = 1, sy = 1, sz = 1;
        public float lx, ly, lz;
    }

    public static final class Bone {
        public String name;
        public String key;
        public String parentName;
        public String binding;
        public int index;
        public int parentIndex = -1;
        public float pivotX, pivotY, pivotZ;
        public float rotX, rotY, rotZ;
        public boolean neverRender;
        public boolean mirror;
        public double inflate;
        public final List<Cube> cubes = new ArrayList<>();
        public final List<Locator> locators = new ArrayList<>();
        public final List<TextureMesh> textureMeshes = new ArrayList<>();
        Map<String, Object> raw;
    }

    public final String id;
    public final int texWidth;
    public final int texHeight;
    public final Bone[] bones;
    public final Map<String, Integer> boneIndex = new HashMap<>();

    private GeometryDef(String id, int tw, int th, List<Bone> ordered) {
        this.id = id;
        this.texWidth = tw;
        this.texHeight = th;
        this.bones = ordered.toArray(new Bone[0]);
        for (int i = 0; i < bones.length; i++) {
            bones[i].index = i;
            boneIndex.put(bones[i].name.toLowerCase(Locale.ROOT), i);
        }
        for (Bone b : bones) {
            if (b.parentName != null) {
                Integer p = boneIndex.get(b.parentName.toLowerCase(Locale.ROOT));
                b.parentIndex = p == null || p == b.index ? -1 : p;
            }
        }
    }

    public Locator findLocator(String name, int[] boneOut) {
        for (Bone b : bones) {
            for (Locator l : b.locators) {
                if (l.name.equalsIgnoreCase(name)) {
                    boneOut[0] = b.index;
                    return l;
                }
            }
        }
        return null;
    }

    public boolean hasBindings() {
        for (Bone b : bones) if (b.binding != null) return true;
        return false;
    }

    public int indexOf(String bone) {
        Integer i = boneIndex.get(bone.toLowerCase(Locale.ROOT));
        return i == null ? -1 : i;
    }

    public static List<GeometryDef> parse(Object rootJson, List<String> problems) {
        Map<String, Object> root = Json.obj(rootJson);
        List<GeometryDef> out = new ArrayList<>();
        if (root == null) return out;
        List<Object> modern = Json.arr(root.get("minecraft:geometry"));
        if (modern != null) {
            for (Object o : modern) {
                Map<String, Object> g = Json.obj(o);
                Map<String, Object> desc = g == null ? null : Json.obj(g.get("description"));
                if (desc == null) continue;
                String id = Json.str(desc.get("identifier"), null);
                if (id == null) continue;
                int tw = (int) Json.num(desc.get("texture_width"), 64);
                int th = (int) Json.num(desc.get("texture_height"), 64);
                add(out, id, tw, th, Json.arr(g.get("bones")), null, problems);
            }
            return out;
        }
        Map<String, Map<String, Object>> legacy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : root.entrySet()) {
            if (e.getKey().startsWith("geometry.") && Json.obj(e.getValue()) != null) {
                legacy.put(e.getKey(), Json.obj(e.getValue()));
            }
        }
        for (Map.Entry<String, Map<String, Object>> e : legacy.entrySet()) {
            String key = e.getKey();
            String parentId = null;
            int colon = key.indexOf(':');
            if (colon > 0) {
                parentId = key.substring(colon + 1);
                key = key.substring(0, colon);
            }
            Map<String, Object> g = e.getValue();
            int tw = (int) Json.num(g.get("texturewidth"), 64);
            int th = (int) Json.num(g.get("textureheight"), 64);
            List<Object> parentBones = null;
            if (parentId != null) {
                for (Map.Entry<String, Map<String, Object>> pe : legacy.entrySet()) {
                    String pk = pe.getKey();
                    int pc = pk.indexOf(':');
                    if ((pc > 0 ? pk.substring(0, pc) : pk).equals(parentId)) {
                        parentBones = Json.arr(pe.getValue().get("bones"));
                        break;
                    }
                }
            }
            add(out, key, tw, th, Json.arr(g.get("bones")), parentBones, problems);
        }
        return out;
    }

    private static void add(
            List<GeometryDef> out, String id, int tw, int th,
            List<Object> bonesJson, List<Object> inherited, List<String> problems) {
        try {
            out.add(build(id, tw, th, bonesJson, inherited));
        } catch (RuntimeException ex) {
            if (problems != null) problems.add("Geometry " + id + ": " + ex);
        }
    }

    private static GeometryDef build(String id, int tw, int th, List<Object> bonesJson, List<Object> inherited) {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        if (inherited != null) {
            for (Object o : inherited) {
                Map<String, Object> b = Json.obj(o);
                if (b != null && b.get("name") instanceof String n) byName.put(n.toLowerCase(Locale.ROOT), b);
            }
        }
        if (bonesJson != null) {
            for (Object o : bonesJson) {
                Map<String, Object> b = Json.obj(o);
                if (b != null && b.get("name") instanceof String n) byName.put(n.toLowerCase(Locale.ROOT), b);
            }
        }
        Map<String, Bone> made = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : byName.entrySet()) {
            made.put(e.getKey(), makeBone(e.getValue(), tw, th));
        }
        List<Bone> ordered = new ArrayList<>();
        Map<String, Boolean> placed = new HashMap<>();
        for (String key : made.keySet()) place(key, made, ordered, placed, 0);
        return new GeometryDef(id, tw, th, ordered);
    }

    private static void place(String key, Map<String, Bone> made, List<Bone> ordered, Map<String, Boolean> placed, int depth) {
        if (placed.containsKey(key) || depth > 256) return;
        Bone b = made.get(key);
        if (b.parentName != null) {
            String pk = b.parentName.toLowerCase(Locale.ROOT);
            if (made.containsKey(pk) && !pk.equals(key)) place(pk, made, ordered, placed, depth + 1);
        }
        if (placed.put(key, Boolean.TRUE) == null) ordered.add(b);
    }

    private static double[] vec(Object o, double d) {
        List<Object> l = Json.arr(o);
        double[] v = {d, d, d};
        if (l != null) for (int i = 0; i < 3 && i < l.size(); i++) v[i] = Json.num(l.get(i), d);
        return v;
    }

    private static Bone makeBone(Map<String, Object> j, int tw, int th) {
        Bone b = new Bone();
        b.raw = j;
        b.name = Json.str(j.get("name"), "");
        b.key = b.name.toLowerCase(Locale.ROOT);
        b.binding = Json.str(j.get("binding"), null);
        b.parentName = Json.str(j.get("parent"), null);
        double[] pivot = vec(j.get("pivot"), 0);
        double[] rot = vec(j.containsKey("rotation") ? j.get("rotation") : j.get("bind_pose_rotation"), 0);
        b.pivotX = (float) -pivot[0];
        b.pivotY = (float) pivot[1];
        b.pivotZ = (float) pivot[2];
        b.rotX = (float) Math.toRadians(-rot[0]);
        b.rotY = (float) Math.toRadians(-rot[1]);
        b.rotZ = (float) Math.toRadians(rot[2]);
        b.neverRender = Json.bool(j.get("neverrender"), Json.bool(j.get("neverRender"), false));
        b.mirror = Json.bool(j.get("mirror"), false);
        b.inflate = Json.num(j.get("inflate"), 0);
        List<Object> cubes = Json.arr(j.get("cubes"));
        if (cubes != null) {
            for (Object c : cubes) {
                Map<String, Object> cj = Json.obj(c);
                if (cj != null) b.cubes.add(makeCube(cj, b, tw, th));
            }
        }
        List<Object> meshes = Json.arr(j.get("texture_meshes"));
        if (meshes != null) {
            for (Object o : meshes) {
                Map<String, Object> mm = Json.obj(o);
                if (mm == null || !(mm.get("texture") instanceof String tex)) continue;
                TextureMesh t = new TextureMesh();
                t.texture = tex.toLowerCase(Locale.ROOT);
                double[] meshPos = vec(mm.get("position"), 0);
                double[] meshRot = vec(mm.get("rotation"), 0);
                double[] meshPivot = vec(mm.get("local_pivot"), 0);
                double[] meshScale = mm.get("scale") instanceof Double d ? new double[] {d, d, d} : vec(mm.get("scale"), 1);
                t.px = (float) -meshPos[0];
                t.py = (float) meshPos[1];
                t.pz = (float) meshPos[2];
                t.rx = (float) Math.toRadians(-meshRot[0]);
                t.ry = (float) Math.toRadians(-meshRot[1]);
                t.rz = (float) Math.toRadians(meshRot[2]);
                t.sx = (float) meshScale[0];
                t.sy = (float) meshScale[1];
                t.sz = (float) meshScale[2];
                t.lx = (float) -meshPivot[0];
                t.ly = (float) meshPivot[1];
                t.lz = (float) meshPivot[2];
                b.textureMeshes.add(t);
            }
        }
        Map<String, Object> locs = Json.obj(j.get("locators"));
        if (locs != null) {
            for (Map.Entry<String, Object> e : locs.entrySet()) {
                Object v = e.getValue();
                Map<String, Object> lo = Json.obj(v);
                double[] p = vec(lo != null ? lo.get("offset") : v, 0);
                b.locators.add(new Locator(e.getKey(), (float) -p[0], (float) p[1], (float) p[2]));
            }
        }
        return b;
    }

    private static Cube makeCube(Map<String, Object> j, Bone bone, int tw, int th) {
        Cube c = new Cube();
        boolean mirror = j.containsKey("mirror") ? Json.bool(j.get("mirror"), false) : bone.mirror;
        double inflate = j.containsKey("inflate") ? Json.num(j.get("inflate"), 0) : bone.inflate;
        double[] size = vec(j.get("size"), 0);
        double[] o = vec(j.get("origin"), 0);
        double[] rot = vec(j.get("rotation"), 0);
        double[] pivot = vec(j.get("pivot"), 0);
        double ox = -(o[0] + size[0]) / 16.0, oy = o[1] / 16.0, oz = o[2] / 16.0;
        double sx = size[0] / 16.0, sy = size[1] / 16.0, sz = size[2] / 16.0;
        double g = inflate / 16.0;
        c.pivotX = (float) -pivot[0];
        c.pivotY = (float) pivot[1];
        c.pivotZ = (float) pivot[2];
        c.rotX = (float) Math.toRadians(-rot[0]);
        c.rotY = (float) Math.toRadians(-rot[1]);
        c.rotZ = (float) Math.toRadians(rot[2]);
        c.rotated = c.rotX != 0 || c.rotY != 0 || c.rotZ != 0;

        double[][] v = new double[8][];
        v[0] = new double[] {ox - g, oy - g, oz - g};
        v[1] = new double[] {ox - g, oy - g, oz + sz + g};
        v[2] = new double[] {ox - g, oy + sy + g, oz - g};
        v[3] = new double[] {ox - g, oy + sy + g, oz + sz + g};
        v[4] = new double[] {ox + sx + g, oy + sy + g, oz - g};
        v[5] = new double[] {ox + sx + g, oy + sy + g, oz + sz + g};
        v[6] = new double[] {ox + sx + g, oy - g, oz - g};
        v[7] = new double[] {ox + sx + g, oy - g, oz + sz + g};
        int bottomLeftBack = 0, bottomRightBack = 1, topLeftBack = 2, topRightBack = 3;
        int topLeftFront = 4, topRightFront = 5, bottomLeftFront = 6, bottomRightFront = 7;
        int[][] faceVerts = new int[6][];
        faceVerts[NORTH] = new int[] {topLeftBack, topLeftFront, bottomLeftFront, bottomLeftBack};
        faceVerts[SOUTH] = new int[] {topRightFront, topRightBack, bottomRightBack, bottomRightFront};
        int[] west = {topRightBack, topLeftBack, bottomLeftBack, bottomRightBack};
        int[] east = {topLeftFront, topRightFront, bottomRightFront, bottomLeftFront};
        int[] up = {topRightBack, topRightFront, topLeftFront, topLeftBack};
        int[] down = {bottomLeftBack, bottomLeftFront, bottomRightFront, bottomRightBack};

        Object uvJson = j.get("uv");
        Map<String, Object> faceUv = Json.obj(uvJson);
        boolean box = faceUv == null;
        double[] boxUv = box ? vec(uvJson, 0) : null;
        double fw = Math.floor(size[0]), fh = Math.floor(size[1]), fd = Math.floor(size[2]);

        for (int dir = 0; dir < 6; dir++) {
            int[] verts;
            switch (dir) {
                case WEST -> verts = mirror ? east : west;
                case EAST -> verts = mirror ? west : east;
                case UP -> verts = mirror && !box ? down : up;
                case DOWN -> verts = mirror && !box ? up : down;
                default -> verts = faceVerts[dir];
            }
            double u, vv, us, vs;
            int rotation = 0;
            String material = null;
            if (box) {
                switch (dir) {
                    case WEST -> {
                        u = boxUv[0] + fd + fw;
                        vv = boxUv[1] + fd;
                        us = fd;
                        vs = fh;
                    }
                    case EAST -> {
                        u = boxUv[0];
                        vv = boxUv[1] + fd;
                        us = fd;
                        vs = fh;
                    }
                    case NORTH -> {
                        u = boxUv[0] + fd;
                        vv = boxUv[1] + fd;
                        us = fw;
                        vs = fh;
                    }
                    case SOUTH -> {
                        u = boxUv[0] + fd + fw + fd;
                        vv = boxUv[1] + fd;
                        us = fw;
                        vs = fh;
                    }
                    case UP -> {
                        u = boxUv[0] + fd;
                        vv = boxUv[1];
                        us = fw;
                        vs = fd;
                    }
                    default -> {
                        u = boxUv[0] + fd + fw;
                        vv = boxUv[1] + fd;
                        us = fw;
                        vs = -fd;
                    }
                }
            } else {
                String key = switch (dir) {
                    case NORTH -> "north";
                    case SOUTH -> "south";
                    case EAST -> "east";
                    case WEST -> "west";
                    case UP -> "up";
                    default -> "down";
                };
                Map<String, Object> f = Json.obj(faceUv.get(key));
                if (f == null) continue;
                double[] uv = vec(f.get("uv"), 0);
                double[] uvs = vec(f.get("uv_size"), 0);
                u = uv[0];
                vv = uv[1];
                us = uvs[0];
                vs = uvs[1];
                rotation = ((int) Json.num(f.get("uv_rotation"), 0) % 360) / 90;
                material = Json.str(f.get("material_instance"), null);
            }
            Quad q = new Quad();
            q.materialInstance = material;
            for (int k = 0; k < 4; k++) {
                double[] p = v[verts[k]];
                q.pos[k * 3] = (float) p[0];
                q.pos[k * 3 + 1] = (float) p[1];
                q.pos[k * 3 + 2] = (float) p[2];
            }
            float u0 = (float) ((u + us) / tw);
            float v1 = (float) ((vv + vs) / th);
            float u1 = (float) (u / tw);
            float v0 = (float) (vv / th);
            if (!mirror) {
                float t = u0;
                u0 = u1;
                u1 = t;
            }
            float[] uvs8 = rotateUvs(rotation, u1, v0, u0, v1);
            System.arraycopy(uvs8, 0, q.uv, 0, 8);
            float nx = 0, ny = 0, nz = 0;
            switch (dir) {
                case NORTH -> nz = -1;
                case SOUTH -> nz = 1;
                case EAST -> nx = 1;
                case WEST -> nx = -1;
                case UP -> ny = 1;
                default -> ny = -1;
            }
            if (mirror) nx = -nx;
            q.normal[0] = nx;
            q.normal[1] = ny;
            q.normal[2] = nz;
            if (!degenerate(q)) c.quads[dir] = q;
        }
        return c;
    }

    private static float[] rotateUvs(int rotation, float u, float v, float uw, float vh) {
        return switch (rotation) {
            case 1 -> new float[] {uw, v, uw, vh, u, vh, u, v};
            case 2 -> new float[] {uw, vh, u, vh, u, v, uw, v};
            case 3 -> new float[] {u, vh, u, v, uw, v, uw, vh};
            default -> new float[] {u, v, uw, v, uw, vh, u, vh};
        };
    }

    private static boolean degenerate(Quad q) {
        float d1x = q.pos[6] - q.pos[0], d1y = q.pos[7] - q.pos[1], d1z = q.pos[8] - q.pos[2];
        float d2x = q.pos[9] - q.pos[3], d2y = q.pos[10] - q.pos[4], d2z = q.pos[11] - q.pos[5];
        float cx = d1y * d2z - d1z * d2y;
        float cy = d1z * d2x - d1x * d2z;
        float cz = d1x * d2y - d1y * d2x;
        return cx * cx + cy * cy + cz * cz < 1.0e-20f;
    }

    public int cubeCount() {
        int n = 0;
        for (Bone b : bones) n += b.cubes.size();
        return n;
    }
}
