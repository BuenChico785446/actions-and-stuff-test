package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Symbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class RenderControllerDef {

    public static final class PatternScript {
        public final String pattern;
        public final Pattern regex;
        public final Script script;

        PatternScript(String pattern, Script script) {
            this.pattern = pattern.toLowerCase(Locale.ROOT);
            StringBuilder sb = new StringBuilder();
            for (char c : this.pattern.toCharArray()) {
                if (c == '*') sb.append(".*");
                else sb.append(Pattern.quote(String.valueOf(c)));
            }
            this.regex = Pattern.compile(sb.toString());
            this.script = script;
        }

        public boolean matches(String bone) {
            return regex.matcher(bone).matches();
        }
    }

    public String id;
    public Script geometry;
    public final List<Script> textures = new ArrayList<>();
    public final List<PatternScript> materials = new ArrayList<>();
    public final List<PatternScript> partVisibility = new ArrayList<>();
    public Script[] color;
    public Script[] overlayColor;
    public Script[] hurtColor;
    public Script[] onFireColor;
    public Script ignoreLighting;
    public Script lightMultiplier;
    public Script[] uvOffset;
    public Script[] uvScale;
    public final Map<String, Object[]> arrays = new LinkedHashMap<>();
    final Map<GeometryDef, int[][]> bindings = new ConcurrentHashMap<>();

    int[][] bind(GeometryDef g) {
        return bindings.computeIfAbsent(g, geo -> {
            int n = geo.bones.length;
            int[] mat = new int[n];
            int[] vis = new int[n];
            for (int i = 0; i < n; i++) {
                String bone = geo.bones[i].name.toLowerCase(Locale.ROOT);
                mat[i] = -1;
                vis[i] = -1;
                for (int k = 0; k < materials.size(); k++) if (materials.get(k).matches(bone)) mat[i] = k;
                for (int k = 0; k < partVisibility.size(); k++) if (partVisibility.get(k).matches(bone)) vis[i] = k;
            }
            return new int[][] {mat, vis};
        });
    }

    public static Map<String, RenderControllerDef> parse(Object rootJson, Symbols symbols, List<String> problems) {
        Map<String, RenderControllerDef> out = new LinkedHashMap<>();
        Map<String, Object> root = Json.obj(rootJson);
        Map<String, Object> rcs = root == null ? null : Json.obj(root.get("render_controllers"));
        if (rcs == null) return out;
        for (Map.Entry<String, Object> e : rcs.entrySet()) {
            Map<String, Object> c = Json.obj(e.getValue());
            if (c == null) continue;
            try {
                RenderControllerDef d = new RenderControllerDef();
                d.id = e.getKey();
                d.geometry = expr(c.get("geometry"), symbols, problems);
                List<Object> texs = Json.arr(c.get("textures"));
                if (texs != null) for (Object t : texs) d.textures.add(expr(t, symbols, problems));
                patterns(c.get("materials"), d.materials, symbols, problems);
                patterns(c.get("part_visibility"), d.partVisibility, symbols, problems);
                d.color = rgba(c.get("color"), symbols, problems);
                d.overlayColor = rgba(c.get("overlay_color"), symbols, problems);
                d.hurtColor = rgba(c.get("is_hurt_color"), symbols, problems);
                d.onFireColor = rgba(c.get("on_fire_color"), symbols, problems);
                Object lm = c.get("light_color_multiplier");
                if (lm instanceof Double || lm instanceof String) d.lightMultiplier = expr(lm, symbols, problems);
                d.ignoreLighting = c.get("ignore_lighting") == null ? null : expr(c.get("ignore_lighting"), symbols, problems);
                Map<String, Object> uv = Json.obj(c.get("uv_anim"));
                if (uv != null) {
                    d.uvOffset = pair(uv.get("offset"), symbols, problems);
                    d.uvScale = pair(uv.get("scale"), symbols, problems);
                }
                arrays(c.get("arrays"), d.arrays);
                out.put(d.id.toLowerCase(Locale.ROOT), d);
            } catch (RuntimeException ex) {
                if (problems != null) problems.add("RenderController " + e.getKey() + ": " + ex);
            }
        }
        return out;
    }

    private static Script expr(Object o, Symbols symbols, List<String> problems) {
        if (o instanceof String s) return Script.compile(s, symbols, problems);
        if (o instanceof Double d) return Script.constant(d);
        if (o instanceof Boolean b) return Script.constant(b ? 1 : 0);
        return Script.constant(0);
    }

    private static void patterns(Object o, List<PatternScript> out, Symbols symbols, List<String> problems) {
        List<Object> l = Json.arr(o);
        if (l == null) return;
        for (Object item : l) {
            Map<String, Object> m = Json.obj(item);
            if (m == null) continue;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                out.add(new PatternScript(e.getKey(), expr(e.getValue(), symbols, problems)));
            }
        }
    }

    private static Script[] rgba(Object o, Symbols symbols, List<String> problems) {
        Map<String, Object> m = Json.obj(o);
        if (m == null) return null;
        return new Script[] {
            expr(m.getOrDefault("r", 0.0), symbols, problems),
            expr(m.getOrDefault("g", 0.0), symbols, problems),
            expr(m.getOrDefault("b", 0.0), symbols, problems),
            expr(m.getOrDefault("a", 1.0), symbols, problems)
        };
    }

    private static Script[] pair(Object o, Symbols symbols, List<String> problems) {
        List<Object> l = Json.arr(o);
        if (l == null || l.size() < 2) return null;
        return new Script[] {expr(l.get(0), symbols, problems), expr(l.get(1), symbols, problems)};
    }

    private static void arrays(Object o, Map<String, Object[]> out) {
        Map<String, Object> groups = Json.obj(o);
        if (groups == null) return;
        for (Object g : groups.values()) {
            Map<String, Object> entries = Json.obj(g);
            if (entries == null) continue;
            for (Map.Entry<String, Object> e : entries.entrySet()) {
                List<Object> items = Json.arr(e.getValue());
                if (items == null) continue;
                Object[] arr = new Object[items.size()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = items.get(i) instanceof String s ? s.toLowerCase(Locale.ROOT) : "";
                }
                out.put(e.getKey().toLowerCase(Locale.ROOT), arr);
            }
        }
    }
}
