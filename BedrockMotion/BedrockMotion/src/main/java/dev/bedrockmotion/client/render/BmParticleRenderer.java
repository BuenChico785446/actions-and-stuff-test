package dev.bedrockmotion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.bedrockmotion.bedrock.ParticleDef;
import dev.bedrockmotion.bedrock.ParticleSystem;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BmParticleRenderer {

    private static final int FULL_BRIGHT = 15728880;
    private static boolean orientationFailed;
    private static int budget;
    private static final int MAX_PARTICLE_QUADS = 6000;

    private BmParticleRenderer() {}

    public static void submit(
            List<ParticleSystem.EmitterView> views,
            Identifier[] textures,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Object camera) {
        float[] right = new float[3];
        float[] up = new float[3];
        basis(camera, right, up);
        budget = MAX_PARTICLE_QUADS;
        for (int i = 0; i < views.size(); i++) {
            ParticleSystem.EmitterView view = views.get(i);
            String material = view.def.material.toLowerCase(Locale.ROOT);
            boolean additive = material.contains("add");
            RenderType type = additive
                    ? RenderTypes.entityTranslucentEmissive(textures[i])
                    : RenderTypes.entityTranslucent(textures[i]);
            collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> write(view, right, up, pose, consumer));
        }
    }

    private static void basis(Object camera, float[] right, float[] up) {
        if (camera != null && !orientationFailed) {
            try {
                Object q = camera.getClass().getField("orientation").get(camera);
                if (q instanceof Quaternionf quaternion) {
                    Vector3f r = new Vector3f(1, 0, 0).rotate(quaternion);
                    Vector3f u = new Vector3f(0, 1, 0).rotate(quaternion);
                    right[0] = r.x;
                    right[1] = r.y;
                    right[2] = r.z;
                    up[0] = u.x;
                    up[1] = u.y;
                    up[2] = u.z;
                    return;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                orientationFailed = true;
            }
        }
        double yaw = 0;
        double pitch = 0;
        if (Minecraft.getInstance().player != null) {
            yaw = Math.toRadians(Minecraft.getInstance().player.getYRot());
            pitch = Math.toRadians(Minecraft.getInstance().player.getXRot());
        }
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw), ry = 0, rz = -Math.sin(yaw);
        right[0] = (float) rx;
        right[1] = (float) ry;
        right[2] = (float) rz;
        up[0] = (float) (ry * fz - rz * fy);
        up[1] = (float) (rz * fx - rx * fz);
        up[2] = (float) (rx * fy - ry * fx);
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static void normalize(float[] v) {
        float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len < 1.0e-6f) return;
        v[0] /= len;
        v[1] /= len;
        v[2] /= len;
    }

    private static void write(
            ParticleSystem.EmitterView view,
            float[] camRight,
            float[] camUp,
            PoseStack.Pose pose,
            VertexConsumer consumer) {
        float[] camBack = cross(camRight, camUp);
        Vector3f v = new Vector3f();
        Vector3f n = new Vector3f();
        int facing = view.def.facing;
        for (ParticleSystem.RenderParticle p : view.particles) {
            if (budget-- <= 0) return;
            if (!Double.isFinite(p.x) || !Double.isFinite(p.y) || !Double.isFinite(p.z)
                    || !Float.isFinite(p.width) || !Float.isFinite(p.height) || !Float.isFinite(p.roll)
                    || Math.abs(p.width) > 16f || Math.abs(p.height) > 16f
                    || Math.abs(p.x) > 256 || Math.abs(p.y) > 256 || Math.abs(p.z) > 256) {
                continue;
            }
            float[] r;
            float[] u;
            float[] back = camBack;
            switch (facing) {
                case ParticleDef.FACE_Y -> {
                    r = new float[] {camRight[0], 0, camRight[2]};
                    normalize(r);
                    u = new float[] {0, 1, 0};
                }
                case ParticleDef.FACE_XY -> {
                    r = new float[] {1, 0, 0};
                    u = new float[] {0, 1, 0};
                    back = new float[] {0, 0, 1};
                }
                case ParticleDef.FACE_XZ -> {
                    r = new float[] {1, 0, 0};
                    u = new float[] {0, 0, 1};
                    back = new float[] {0, 1, 0};
                }
                case ParticleDef.FACE_YZ -> {
                    r = new float[] {0, 0, 1};
                    u = new float[] {0, 1, 0};
                    back = new float[] {1, 0, 0};
                }
                case ParticleDef.FACE_DIRECTION -> {
                    u = new float[] {(float) p.vx, (float) p.vy, (float) p.vz};
                    if (u[0] * u[0] + u[1] * u[1] + u[2] * u[2] < 1.0e-8f) {
                        u = camUp.clone();
                        r = camRight.clone();
                    } else {
                        normalize(u);
                        r = cross(u, camBack);
                        normalize(r);
                    }
                }
                default -> {
                    r = camRight.clone();
                    u = camUp.clone();
                }
            }
            if (p.roll != 0 && facing != ParticleDef.FACE_DIRECTION) {
                double a = Math.toRadians(p.roll);
                float c = (float) Math.cos(a), s = (float) Math.sin(a);
                float[] nr = {r[0] * c + u[0] * s, r[1] * c + u[1] * s, r[2] * c + u[2] * s};
                float[] nu = {-r[0] * s + u[0] * c, -r[1] * s + u[1] * c, -r[2] * s + u[2] * c};
                r = nr;
                u = nu;
            }
            float hw = p.width * 0.5f;
            float hh = p.height * 0.5f;
            pose.normal().transform(back[0], back[1], back[2], n);
            float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
            float[][] uvs = {{p.u0, p.v1}, {p.u1, p.v1}, {p.u1, p.v0}, {p.u0, p.v0}};
            for (int k = 0; k < 4; k++) {
                float sx = corners[k][0] * hw;
                float sy = corners[k][1] * hh;
                float x = (float) p.x + r[0] * sx + u[0] * sy;
                float y = (float) p.y + r[1] * sx + u[1] * sy;
                float z = (float) p.z + r[2] * sx + u[2] * sy;
                pose.pose().transformPosition(x, y, z, v);
                consumer.addVertex(v.x, v.y, v.z)
                        .setColor(p.argb)
                        .setUv(uvs[k][0], uvs[k][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(FULL_BRIGHT)
                        .setNormal(n.x, n.y, n.z);
            }
        }
    }
}
