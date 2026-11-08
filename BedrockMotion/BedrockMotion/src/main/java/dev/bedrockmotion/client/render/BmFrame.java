package dev.bedrockmotion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.bedrockmotion.bedrock.EntityAnimator;
import dev.bedrockmotion.bedrock.GeometryDef;
import dev.bedrockmotion.bedrock.ParticleSystem;
import dev.bedrockmotion.bedrock.Pose;
import dev.bedrockmotion.bedrock.RenderPlan;
import dev.bedrockmotion.client.runtime.BmModelPose;
import dev.bedrockmotion.client.runtime.BmRuntime;
import dev.bedrockmotion.client.runtime.BmTextures;
import dev.bedrockmotion.pack.MaterialKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BmFrame {

    public record Context(boolean hurt, boolean onFire, Identifier skin) {
        public static final Context NONE = new Context(false, false, null);
    }

    public record Owner(GeometryDef geometry, Matrix4f[] mats, Matrix3f[] norms) {}

    private record MeshDraw(int bone, GeometryDef.TextureMesh mesh, Identifier texture) {}

    private static final int FULL_BRIGHT = 15728880;
    private static final MaterialKind[] KINDS = MaterialKind.values();

    private final RenderPlan plan;
    private final Matrix4f[][] mats;
    private final Matrix3f[][] norms;
    private final Identifier[] textures;
    private final Identifier[][] extraTextures;
    private final int[] colors;
    private final int[] overlays;
    private final List<List<MeshDraw>> meshes;
    private final float yaw;
    private final BmModelPose driver;
    private final List<ParticleSystem.EmitterView> particles;
    private final Identifier[] particleTextures;
    private List<BmFrame> attachments = List.of();

    private BmFrame(
            RenderPlan plan,
            Matrix4f[][] mats,
            Matrix3f[][] norms,
            Identifier[] textures,
            Identifier[][] extraTextures,
            int[] colors,
            int[] overlays,
            List<List<MeshDraw>> meshes,
            float yaw,
            BmModelPose driver,
            List<ParticleSystem.EmitterView> particles,
            Identifier[] particleTextures) {
        this.plan = plan;
        this.mats = mats;
        this.norms = norms;
        this.textures = textures;
        this.extraTextures = extraTextures;
        this.colors = colors;
        this.overlays = overlays;
        this.meshes = meshes;
        this.yaw = yaw;
        this.driver = driver;
        this.particles = particles;
        this.particleTextures = particleTextures;
    }

    public boolean hasParticles() {
        return !particles.isEmpty();
    }

    public void submitParticles(PoseStack poseStack, SubmitNodeCollector collector, Object camera) {
        BmParticleRenderer.submit(particles, particleTextures, poseStack, collector, camera);
    }

    public void setAttachments(List<BmFrame> attachments) {
        this.attachments = attachments;
    }

    public Owner owner() {
        return plan.layers.isEmpty() ? null : new Owner(plan.layers.get(0).geometry, mats[0], norms[0]);
    }

    public boolean replacesModel() {
        return !plan.layers.isEmpty();
    }

    public BmModelPose driver() {
        return driver;
    }

    public static BmFrame build(
            EntityAnimator animator,
            RenderPlan plan,
            float yaw,
            Context ctx,
            BmModelPose driver,
            List<ParticleSystem.EmitterView> particles,
            Owner owner) {
        plan.scaleX = sane(plan.scaleX);
        plan.scaleY = sane(plan.scaleY);
        plan.scaleZ = sane(plan.scaleZ);
        int n = plan.layers.size();
        Matrix4f[][] mats = new Matrix4f[n][];
        Matrix3f[][] norms = new Matrix3f[n][];
        Identifier[] textures = new Identifier[n];
        Identifier[][] extra = new Identifier[n][];
        int[] colors = new int[n];
        int[] overlays = new int[n];
        List<List<MeshDraw>> meshes = new ArrayList<>();
        int hurtOverlay = OverlayTexture.pack(OverlayTexture.u(0f), OverlayTexture.v(true));
        for (int i = 0; i < n; i++) {
            RenderPlan.Layer layer = plan.layers.get(i);
            GeometryDef g = layer.geometry;
            mats[i] = new Matrix4f[g.bones.length];
            norms[i] = new Matrix3f[g.bones.length];
            compute(g, animator.pose, mats[i], norms[i], layer.bindings, owner);
            textures[i] = i == 0 && ctx.skin() != null ? ctx.skin() : BmTextures.get(animator.def.pack, layer.texturePath);
            extra[i] = new Identifier[layer.extraTexturePaths.size()];
            for (int k = 0; k < extra[i].length; k++) {
                extra[i][k] = BmTextures.get(animator.def.pack, layer.extraTexturePaths.get(k));
            }
            colors[i] = color(layer, ctx.hurt(), ctx.onFire());
            overlays[i] = ctx.hurt() && layer.hurtA <= 0 ? hurtOverlay : OverlayTexture.NO_OVERLAY;
            List<MeshDraw> draws = new ArrayList<>();
            for (GeometryDef.Bone b : g.bones) {
                if (!BmRuntime.features.textureMeshes) break;
                for (GeometryDef.TextureMesh t : b.textureMeshes) {
                    String path = animator.def.textures.get(t.texture);
                    if (path != null) draws.add(new MeshDraw(b.index, t, BmTextures.get(animator.def.pack, path)));
                }
            }
            meshes.add(draws);
        }
        List<ParticleSystem.EmitterView> drawable = new ArrayList<>();
        List<Identifier> particleIds = new ArrayList<>();
        for (ParticleSystem.EmitterView view : particles) {
            Identifier id = BmTextures.get(animator.def.pack, view.def.texture);
            if (BmTextures.isFallback(id)) continue;
            drawable.add(view);
            particleIds.add(id);
        }
        return new BmFrame(
                plan, mats, norms, textures, extra, colors, overlays, meshes, yaw, driver,
                drawable, particleIds.toArray(new Identifier[0]));
    }

    private static double sane(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 1;
        return Math.max(0.001, Math.min(32, v));
    }

    private static int color(RenderPlan.Layer l, boolean hurt, boolean fire) {
        float r = l.r * l.lightMultiplier;
        float g = l.g * l.lightMultiplier;
        float b = l.b * l.lightMultiplier;
        if (hurt && l.hurtA > 0) {
            r = mix(r, l.hurtR, l.hurtA);
            g = mix(g, l.hurtG, l.hurtA);
            b = mix(b, l.hurtB, l.hurtA);
        }
        if (fire && l.fireA > 0) {
            r = mix(r, l.fireR, l.fireA);
            g = mix(g, l.fireG, l.fireA);
            b = mix(b, l.fireB, l.fireA);
        }
        if (l.overlayA > 0) {
            r = mix(r, l.overlayR, l.overlayA);
            g = mix(g, l.overlayG, l.overlayA);
            b = mix(b, l.overlayB, l.overlayA);
        }
        return (channel(l.a) << 24) | (channel(r) << 16) | (channel(g) << 8) | channel(b);
    }

    private static float mix(float a, float b, float t) {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }

    private static void compute(
            GeometryDef g, Pose pose, Matrix4f[] mats, Matrix3f[] norms, String[] bindings, Owner owner) {
        for (int i = 0; i < g.bones.length; i++) {
            GeometryDef.Bone b = g.bones[i];
            Matrix4f m = new Matrix4f();
            Matrix3f nm = new Matrix3f();
            int oi = bindings != null && bindings[i] != null && owner != null ? owner.geometry().indexOf(bindings[i]) : -1;
            if (oi >= 0) {
                GeometryDef.Bone ob = owner.geometry().bones[oi];
                m.set(owner.mats()[oi]);
                nm.set(owner.norms()[oi]);
                m.translate(ob.pivotX / 16f, ob.pivotY / 16f, ob.pivotZ / 16f);
            } else if (b.parentIndex >= 0) {
                m.set(mats[b.parentIndex]);
                nm.set(norms[b.parentIndex]);
            }
            Pose.Xf xf = pose.find(b.key);
            float ax = 0, ay = 0, az = 0;
            float sx = 1, sy = 1, sz = 1;
            if (xf != null) {
                m.translate((float) (xf.px / 16.0), (float) (xf.py / 16.0), (float) (xf.pz / 16.0));
                ax = (float) xf.rx;
                ay = (float) xf.ry;
                az = (float) xf.rz;
                sx = (float) xf.sx;
                sy = (float) xf.sy;
                sz = (float) xf.sz;
            }
            float px = b.pivotX / 16f, py = b.pivotY / 16f, pz = b.pivotZ / 16f;
            m.translate(px, py, pz);
            Matrix3f local = new Matrix3f();
            if (xf != null && xf.entityRotation && b.parentIndex >= 0) {
                Matrix3f parent = norms[b.parentIndex];
                local.set(parent).invert();
                local.mul(new Matrix3f().rotateZ(az).rotateY(ay).rotateX(ax));
                local.mul(parent);
                local.rotateZ(b.rotZ).rotateY(b.rotY).rotateX(b.rotX);
            } else {
                local.rotateZ(b.rotZ + az).rotateY(b.rotY + ay).rotateX(b.rotX + ax);
            }
            m.mul(new Matrix4f(local));
            nm.mul(local);
            if (sx != 1 || sy != 1 || sz != 1) m.scale(sx, sy, sz);
            m.translate(-px, -py, -pz);
            if (!m.isFinite()) m.identity();
            if (!nm.isFinite()) nm.identity();
            mats[i] = m;
            norms[i] = nm;
        }
    }

    public void submit(int light, PoseStack poseStack, SubmitNodeCollector collector, boolean applyYaw) {
        poseStack.pushPose();
        if (applyYaw) poseStack.rotate(Axis.YP.rotationDegrees(180f - yaw));
        poseStack.scale((float) plan.scaleX, (float) plan.scaleY, (float) plan.scaleZ);
        for (int li = 0; li < plan.layers.size(); li++) {
            RenderPlan.Layer layer = plan.layers.get(li);
            Matrix4f[] lm = mats[li];
            Matrix3f[] ln = norms[li];
            Identifier tex = textures[li];
            int color = colors[li];
            int ov = overlays[li];
            boolean[] present = new boolean[KINDS.length];
            for (int b = 0; b < layer.boneKinds.length; b++) {
                if (layer.boneVisible[b] && !layer.geometry.bones[b].cubes.isEmpty()) present[layer.boneKinds[b].ordinal()] = true;
            }
            for (MaterialKind kind : KINDS) {
                if (!present[kind.ordinal()]) continue;
                RenderType type = renderType(kind, layer, tex);
                int lightFinal = layer.ignoreLighting || kind == MaterialKind.EMISSIVE || kind == MaterialKind.EYES ? FULL_BRIGHT : light;
                collector.submitCustomGeometry(
                        poseStack, type, (pose, consumer) -> write(layer, lm, ln, kind, color, lightFinal, ov, pose, consumer));
            }
            for (Identifier extraTex : extraTextures[li]) {
                collector.submitCustomGeometry(
                        poseStack,
                        RenderTypes.entityTranslucent(extraTex),
                        (pose, consumer) -> write(layer, lm, ln, null, color, light, ov, pose, consumer));
            }
            for (MeshDraw draw : meshes.get(li)) {
                if (!layer.boneVisible[draw.bone()]) continue;
                int lightFinal = layer.ignoreLighting ? FULL_BRIGHT : light;
                collector.submitCustomGeometry(
                        poseStack,
                        RenderTypes.entityCutout(draw.texture()),
                        (pose, consumer) -> writeMesh(lm[draw.bone()], draw.mesh(), color, lightFinal, pose, consumer));
            }
        }
        poseStack.popPose();
        for (BmFrame attachment : attachments) attachment.submit(light, poseStack, collector, applyYaw);
    }

    private static RenderType renderType(MaterialKind kind, RenderPlan.Layer layer, Identifier tex) {
        return switch (kind) {
            case EMISSIVE, EYES -> RenderTypes.entityTranslucentEmissive(tex);
            case TRANSLUCENT -> RenderTypes.entityTranslucent(tex);
            default -> layer.a < 0.99f ? RenderTypes.entityTranslucent(tex) : RenderTypes.entityCutout(tex);
        };
    }

    private static int channel(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255f)));
    }

    private static void writeMesh(
            Matrix4f bone, GeometryDef.TextureMesh t, int color, int light, PoseStack.Pose pose, VertexConsumer consumer) {
        Matrix4f m = new Matrix4f(pose.pose()).mul(bone);
        m.translate(t.px / 16f, t.py / 16f, t.pz / 16f);
        m.translate(t.lx / 16f, t.ly / 16f, t.lz / 16f);
        m.rotateZ(t.rz).rotateY(t.ry).rotateX(t.rx);
        m.translate(-t.lx / 16f, -t.ly / 16f, -t.lz / 16f);
        m.scale(t.sx, t.sy, t.sz);
        Vector3f v = new Vector3f();
        Vector3f front = new Vector3f();
        pose.normal().transform(0, 0, -1, front);
        float[][] corners = {{-0.5f, -0.5f}, {0.5f, -0.5f}, {0.5f, 0.5f}, {-0.5f, 0.5f}};
        float[][] uvs = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int side = 0; side < 2; side++) {
            for (int k = 0; k < 4; k++) {
                int idx = side == 0 ? k : 3 - k;
                m.transformPosition(corners[idx][0], corners[idx][1], 0, v);
                float flip = side == 0 ? 1f : -1f;
                consumer.addVertex(v.x, v.y, v.z)
                        .setColor(color)
                        .setUv(uvs[idx][0], uvs[idx][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(front.x * flip, front.y * flip, front.z * flip);
            }
        }
    }

    private static void write(
            RenderPlan.Layer layer,
            Matrix4f[] mats,
            Matrix3f[] norms,
            MaterialKind kind,
            int color,
            int light,
            int overlay,
            PoseStack.Pose pose,
            VertexConsumer consumer) {
        Matrix4f m = new Matrix4f();
        Matrix4f mc = new Matrix4f();
        Matrix3f n = new Matrix3f();
        Matrix3f nc = new Matrix3f();
        Vector3f v = new Vector3f();
        Vector3f nv = new Vector3f();
        GeometryDef g = layer.geometry;
        for (int i = 0; i < g.bones.length; i++) {
            GeometryDef.Bone bone = g.bones[i];
            if (!layer.boneVisible[i] || (kind != null && layer.boneKinds[i] != kind) || bone.cubes.isEmpty()) continue;
            m.set(pose.pose()).mul(mats[i]);
            n.set(pose.normal()).mul(norms[i]);
            for (GeometryDef.Cube cube : bone.cubes) {
                Matrix4f cm = m;
                Matrix3f cn = n;
                if (cube.rotated) {
                    float cx = cube.pivotX / 16f, cy = cube.pivotY / 16f, cz = cube.pivotZ / 16f;
                    mc.set(m).translate(cx, cy, cz).rotateZ(cube.rotZ).rotateY(cube.rotY).rotateX(cube.rotX).translate(-cx, -cy, -cz);
                    nc.set(n).rotateZ(cube.rotZ).rotateY(cube.rotY).rotateX(cube.rotX);
                    cm = mc;
                    cn = nc;
                }
                for (GeometryDef.Quad q : cube.quads) {
                    if (q == null) continue;
                    cn.transform(q.normal[0], q.normal[1], q.normal[2], nv);
                    for (int k = 0; k < 4; k++) {
                        cm.transformPosition(q.pos[k * 3], q.pos[k * 3 + 1], q.pos[k * 3 + 2], v);
                        if (!Float.isFinite(v.x) || !Float.isFinite(v.y) || !Float.isFinite(v.z)) v.set(0, 0, 0);
                        consumer.addVertex(v.x, v.y, v.z)
                                .setColor(color)
                                .setUv(q.uv[k * 2] * layer.uScale + layer.uOffset, q.uv[k * 2 + 1] * layer.vScale + layer.vOffset)
                                .setOverlay(overlay)
                                .setLight(light)
                                .setNormal(nv.x, nv.y, nv.z);
                    }
                }
            }
        }
    }
}
