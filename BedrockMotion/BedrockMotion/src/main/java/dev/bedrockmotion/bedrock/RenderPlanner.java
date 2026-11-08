package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.molang.Env;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Values;
import dev.bedrockmotion.pack.MaterialKind;
import java.util.Locale;
import java.util.Map;

public final class RenderPlanner {

    private RenderPlanner() {}

    public static RenderPlan plan(EntityAnimator animator) {
        ClientEntityDef def = animator.def;
        Env env = animator.env;
        RenderPlan plan = new RenderPlan();
        double uniform = animator.entityScale();
        plan.scaleX = def.scaleX == null ? uniform : def.scaleX.runNum(env) * uniform;
        plan.scaleY = def.scaleY == null ? uniform : def.scaleY.runNum(env) * uniform;
        plan.scaleZ = def.scaleZ == null ? uniform : def.scaleZ.runNum(env) * uniform;
        for (ClientEntityDef.Conditional c : def.renderControllers) {
            if (c.condition() != null && !Values.truthy(c.condition().run(env))) continue;
            RenderControllerDef rc = def.pack.renderController(c.name().toLowerCase(Locale.ROOT));
            if (rc == null) continue;
            RenderPlan.Layer layer = layer(def, rc, env);
            if (layer != null) plan.layers.add(layer);
        }
        env.arrays = Map.of();
        return plan;
    }

    private static String resolve(Map<String, String> map, String ref, String prefix) {
        if (ref == null) return null;
        String key = ref.toLowerCase(Locale.ROOT);
        if (key.startsWith(prefix)) key = key.substring(prefix.length());
        return map.get(key);
    }

    private static RenderPlan.Layer layer(ClientEntityDef def, RenderControllerDef rc, Env env) {
        env.arrays = rc.arrays;
        String geoRef = rc.geometry == null ? null : rc.geometry.runString(env);
        String geoId = resolve(def.geometry, geoRef, "geometry.");
        if (geoId == null && geoRef != null) geoId = geoRef;
        if (geoId == null) return null;
        GeometryDef geo = def.pack.geometry(geoId.toLowerCase(Locale.ROOT));
        if (geo == null) return null;
        RenderPlan.Layer layer = new RenderPlan.Layer();
        layer.geometry = geo;
        if (!rc.textures.isEmpty()) {
            String texRef = rc.textures.get(0).runString(env);
            layer.texturePath = resolve(def.textures, texRef, "texture.");
        }
        int n = geo.bones.length;
        int[][] binding = rc.bind(geo);
        MaterialKind[] kindByPattern = new MaterialKind[rc.materials.size()];
        boolean multi = false;
        for (int k = 0; k < kindByPattern.length; k++) {
            String matRef = rc.materials.get(k).script.runString(env);
            String name = resolve(def.materials, matRef, "material.");
            kindByPattern[k] = def.pack.materialKind(name == null ? "" : name);
            multi |= def.pack.isMultitexture(name);
        }
        if (multi) {
            for (int t = 1; t < rc.textures.size(); t++) {
                String extra = resolve(def.textures, rc.textures.get(t).runString(env), "texture.");
                if (extra != null) layer.extraTexturePaths.add(extra);
            }
        }
        boolean[] visByPattern = new boolean[rc.partVisibility.size()];
        for (int k = 0; k < visByPattern.length; k++) {
            visByPattern[k] = Values.truthy(rc.partVisibility.get(k).script.run(env));
        }
        layer.boneKinds = new MaterialKind[n];
        layer.boneVisible = new boolean[n];
        for (int i = 0; i < n; i++) {
            int m = binding[0][i];
            layer.boneKinds[i] = m < 0 ? MaterialKind.CUTOUT : kindByPattern[m];
            int v = binding[1][i];
            layer.boneVisible[i] = (v < 0 || visByPattern[v]) && !geo.bones[i].neverRender;
        }
        if (geo.hasBindings()) {
            layer.bindings = new String[n];
            for (int i = 0; i < n; i++) {
                String raw = geo.bones[i].binding;
                if (raw == null) continue;
                String bound = Script.compile(raw, def.pack.symbols, null).runString(env);
                layer.bindings[i] = bound == null || bound.isEmpty() ? null : bound;
            }
        }
        if (rc.color != null) {
            layer.r = (float) rc.color[0].runNum(env);
            layer.g = (float) rc.color[1].runNum(env);
            layer.b = (float) rc.color[2].runNum(env);
            layer.a = (float) rc.color[3].runNum(env);
        }
        if (rc.hurtColor != null) {
            layer.hurtR = (float) rc.hurtColor[0].runNum(env);
            layer.hurtG = (float) rc.hurtColor[1].runNum(env);
            layer.hurtB = (float) rc.hurtColor[2].runNum(env);
            layer.hurtA = (float) rc.hurtColor[3].runNum(env);
        }
        if (rc.onFireColor != null) {
            layer.fireR = (float) rc.onFireColor[0].runNum(env);
            layer.fireG = (float) rc.onFireColor[1].runNum(env);
            layer.fireB = (float) rc.onFireColor[2].runNum(env);
            layer.fireA = (float) rc.onFireColor[3].runNum(env);
        }
        if (rc.overlayColor != null) {
            layer.overlayR = (float) rc.overlayColor[0].runNum(env);
            layer.overlayG = (float) rc.overlayColor[1].runNum(env);
            layer.overlayB = (float) rc.overlayColor[2].runNum(env);
            layer.overlayA = (float) rc.overlayColor[3].runNum(env);
        }
        if (rc.lightMultiplier != null) layer.lightMultiplier = (float) Math.max(0, Math.min(4, rc.lightMultiplier.runNum(env)));
        layer.ignoreLighting = rc.ignoreLighting != null && Values.truthy(rc.ignoreLighting.run(env));
        if (rc.uvOffset != null) {
            layer.uOffset = (float) rc.uvOffset[0].runNum(env);
            layer.vOffset = (float) rc.uvOffset[1].runNum(env);
        }
        if (rc.uvScale != null) {
            layer.uScale = (float) rc.uvScale[0].runNum(env);
            layer.vScale = (float) rc.uvScale[1].runNum(env);
        }
        return layer;
    }

}
