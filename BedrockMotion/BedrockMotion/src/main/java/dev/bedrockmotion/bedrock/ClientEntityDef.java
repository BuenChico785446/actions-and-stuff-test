package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Symbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClientEntityDef {

    public record Conditional(String name, Script condition) {}

    public String identifier;
    public boolean attachable;
    public BedrockPack pack;
    public final Map<String, String> materials = new LinkedHashMap<>();
    public final Map<String, String> textures = new LinkedHashMap<>();
    public final Map<String, String> geometry = new LinkedHashMap<>();
    public final Map<String, String> animations = new LinkedHashMap<>();
    public final Map<String, String> soundEffects = new LinkedHashMap<>();
    public final Map<String, String> particleEffects = new LinkedHashMap<>();
    public final List<Conditional> animate = new ArrayList<>();
    public final List<Conditional> renderControllers = new ArrayList<>();
    public Script[] initialize = new Script[0];
    public Script[] preAnimation = new Script[0];
    public Script scale;
    public Script scaleX;
    public Script scaleY;
    public Script scaleZ;
    public boolean enableAttachables;
    public boolean hideArmor;
    public long minEngine;

    public static ClientEntityDef parse(Object rootJson, Symbols symbols, List<String> problems) {
        Map<String, Object> root = Json.obj(rootJson);
        if (root == null) return null;
        boolean attachable = false;
        Map<String, Object> top = Json.obj(root.get("minecraft:client_entity"));
        if (top == null) {
            top = Json.obj(root.get("minecraft:attachable"));
            attachable = true;
        }
        Map<String, Object> d = top == null ? null : Json.obj(top.get("description"));
        if (d == null || !(d.get("identifier") instanceof String id)) return null;
        ClientEntityDef def = new ClientEntityDef();
        def.identifier = id.toLowerCase(Locale.ROOT);
        def.attachable = attachable;
        strings(d.get("materials"), def.materials);
        strings(d.get("textures"), def.textures);
        strings(d.get("geometry"), def.geometry);
        strings(d.get("animations"), def.animations);
        strings(d.get("sound_effects"), def.soundEffects);
        strings(d.get("particle_effects"), def.particleEffects);
        def.enableAttachables = Json.bool(d.get("enable_attachables"), false);
        def.hideArmor = Json.bool(d.get("hide_armor"), false);
        def.minEngine = engineVersion(Json.str(d.get("min_engine_version"), null));
        Map<String, Object> scripts = Json.obj(d.get("scripts"));
        if (scripts != null) {
            def.initialize = scriptList(scripts.get("initialize"), symbols, problems);
            def.preAnimation = scriptList(scripts.get("pre_animation"), symbols, problems);
            def.scale = scalar(scripts.get("scale"), symbols, problems);
            def.scaleX = scalar(scripts.get("scalex"), symbols, problems);
            def.scaleY = scalar(scripts.get("scaley"), symbols, problems);
            def.scaleZ = scalar(scripts.get("scalez"), symbols, problems);
            conditionals(scripts.get("animate"), def.animate, symbols, problems);
        }
        conditionals(d.get("render_controllers"), def.renderControllers, symbols, problems);
        return def;
    }

    private static long engineVersion(String v) {
        if (v == null) return 0;
        long out = 0;
        String[] parts = v.split("\\.");
        for (int i = 0; i < 3; i++) {
            long n = 0;
            if (i < parts.length) {
                try {
                    n = Long.parseLong(parts[i].trim());
                } catch (NumberFormatException ignored) {
                    n = 0;
                }
            }
            out = out * 1000 + Math.min(999, n);
        }
        return out;
    }

    private static void strings(Object o, Map<String, String> out) {
        Map<String, Object> m = Json.obj(o);
        if (m == null) return;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (e.getValue() instanceof String s) out.put(e.getKey().toLowerCase(Locale.ROOT), s);
        }
    }

    private static Script[] scriptList(Object o, Symbols symbols, List<String> problems) {
        List<Object> l = Json.arr(o);
        if (l == null) return new Script[0];
        List<Script> out = new ArrayList<>();
        for (Object x : l) if (x instanceof String s) out.add(Script.compile(s, symbols, problems));
        return out.toArray(new Script[0]);
    }

    private static Script scalar(Object o, Symbols symbols, List<String> problems) {
        if (o instanceof String s) return Script.compile(s, symbols, problems);
        if (o instanceof Double d) return Script.constant(d);
        return null;
    }

    private static void conditionals(Object o, List<Conditional> out, Symbols symbols, List<String> problems) {
        List<Object> l = Json.arr(o);
        if (l == null) return;
        for (Object item : l) {
            if (item instanceof String s) {
                out.add(new Conditional(s, null));
            } else if (Json.obj(item) != null) {
                for (Map.Entry<String, Object> e : Json.obj(item).entrySet()) {
                    Script cond = e.getValue() instanceof String x ? Script.compile(x, symbols, problems)
                            : e.getValue() instanceof Double dv ? Script.constant(dv) : null;
                    out.add(new Conditional(e.getKey(), cond));
                }
            }
        }
    }
}
