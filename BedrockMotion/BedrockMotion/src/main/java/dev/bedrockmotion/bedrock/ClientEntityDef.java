package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Symbols;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    public Script parentSetup;
    public final Set<String> publicVariables = new HashSet<>();
    public boolean enableAttachables;
    public boolean hideArmor;
    public long minEngine;

    public void inheritMissing(ClientEntityDef lower) {
        for (Map.Entry<String, String> e : lower.materials.entrySet()) materials.putIfAbsent(e.getKey(), e.getValue());
        for (Map.Entry<String, String> e : lower.textures.entrySet()) textures.putIfAbsent(e.getKey(), e.getValue());
        for (Map.Entry<String, String> e : lower.geometry.entrySet()) geometry.putIfAbsent(e.getKey(), e.getValue());
        for (Map.Entry<String, String> e : lower.animations.entrySet()) animations.putIfAbsent(e.getKey(), e.getValue());
        for (Map.Entry<String, String> e : lower.soundEffects.entrySet()) soundEffects.putIfAbsent(e.getKey(), e.getValue());
        for (Map.Entry<String, String> e : lower.particleEffects.entrySet()) particleEffects.putIfAbsent(e.getKey(), e.getValue());
        if (renderControllers.isEmpty()) renderControllers.addAll(lower.renderControllers);
        if (animate.isEmpty()) animate.addAll(lower.animate);
        if (initialize.length == 0) initialize = lower.initialize;
        if (preAnimation.length == 0) preAnimation = lower.preAnimation;
        if (scale == null) scale = lower.scale;
        if (scaleX == null) scaleX = lower.scaleX;
        if (scaleY == null) scaleY = lower.scaleY;
        if (scaleZ == null) scaleZ = lower.scaleZ;
        if (parentSetup == null) parentSetup = lower.parentSetup;
        publicVariables.addAll(lower.publicVariables);
        if (!enableAttachables) enableAttachables = lower.enableAttachables;
        if (!hideArmor) hideArmor = lower.hideArmor;
    }

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
            def.initialize = scriptList(script(scripts, "initialize"), symbols, problems);
            def.preAnimation = scriptList(script(scripts, "pre_animation"), symbols, problems);
            def.scale = scalar(script(scripts, "scale"), symbols, problems);
            def.scaleX = scalar(script(scripts, "scalex"), symbols, problems);
            def.scaleY = scalar(script(scripts, "scaley"), symbols, problems);
            def.scaleZ = scalar(script(scripts, "scalez"), symbols, problems);
            def.parentSetup = scalar(script(scripts, "parent_setup"), symbols, problems);
            publicVariables(script(scripts, "variables"), def.publicVariables);
            conditionals(script(scripts, "animate"), def.animate, symbols, problems);
        }
        if (def.animate.isEmpty()) legacyControllers(d.get("animation_controllers"), def);
        conditionals(d.get("render_controllers"), def.renderControllers, symbols, problems);
        return def;
    }

    private static void legacyControllers(Object o, ClientEntityDef def) {
        List<Object> list = Json.arr(o);
        if (list == null) return;
        for (Object item : list) {
            Map<String, Object> entry = Json.obj(item);
            if (entry == null) continue;
            for (Map.Entry<String, Object> e : entry.entrySet()) {
                if (!(e.getValue() instanceof String id)) continue;
                String key = "legacy:" + e.getKey().toLowerCase(Locale.ROOT);
                def.animations.put(key, id);
                def.animate.add(new Conditional(key, null));
            }
        }
    }

    private static Object script(Map<String, Object> scripts, String key) {
        for (Map.Entry<String, Object> e : scripts.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) return e.getValue();
        }
        return null;
    }

    private static void publicVariables(Object o, Set<String> out) {
        Map<String, Object> m = Json.obj(o);
        if (m == null) return;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (!(e.getValue() instanceof String s) || !s.equalsIgnoreCase("public")) continue;
            String name = e.getKey().toLowerCase(Locale.ROOT);
            int dot = name.indexOf('.');
            out.add(dot >= 0 ? name.substring(dot + 1) : name);
        }
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
        List<String> parts = new ArrayList<>();
        for (Object x : l) if (x instanceof String s) parts.add(s);
        // Bedrock joins script array entries, so one statement can span several (vanilla piglin ternary blocks).
        List<Script> out = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            String cur = parts.get(i);
            int j = i;
            while (!Script.parses(cur, symbols) && j + 1 < parts.size()) cur = cur + "\n" + parts.get(++j);
            if (j > i && Script.parses(cur, symbols)) {
                out.add(Script.compile(cur, symbols, problems));
                i = j;
            } else {
                out.add(Script.compile(parts.get(i), symbols, problems));
            }
        }
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
