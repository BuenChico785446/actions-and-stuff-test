package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.molang.Env;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Symbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AnimationDef {

    public enum Loop { NONE, LOOP, HOLD }

    public static final int LINEAR = 0, CATMULL = 1, STEP = 2;

    public static final class Keyframe {
        public double time;
        public Script[] pre;
        public Script[] post;
        public int lerp;
    }

    public static final class Channel {
        public Script[] constant;
        public Keyframe[] frames;

        public void sample(Env env, double t, double[] out, double[] cur) {
            if (constant != null) {
                for (int i = 0; i < 3; i++) {
                    env.thisValue = cur[i];
                    out[i] = constant[i].runNum(env);
                }
                return;
            }
            Keyframe[] f = frames;
            int n = f.length;
            if (n == 0) {
                out[0] = out[1] = out[2] = 0;
                return;
            }
            if (t <= f[0].time) {
                eval(env, f[0].pre, out, cur);
                return;
            }
            if (t >= f[n - 1].time) {
                eval(env, f[n - 1].post, out, cur);
                return;
            }
            int i = 0;
            while (i + 1 < n - 1 && f[i + 1].time <= t) i++;
            Keyframe a = f[i];
            Keyframe b = f[i + 1];
            double span = b.time - a.time;
            double alpha = span <= 0 ? 1 : (t - a.time) / span;
            if (a.lerp == STEP) {
                eval(env, a.post, out, cur);
                return;
            }
            double[] p1 = new double[3];
            double[] p2 = new double[3];
            eval(env, a.post, p1, cur);
            eval(env, b.pre, p2, cur);
            if (a.lerp == CATMULL || b.lerp == CATMULL) {
                double[] p0 = new double[3];
                double[] p3 = new double[3];
                eval(env, i > 0 ? f[i - 1].post : a.post, p0, cur);
                eval(env, i + 2 < n ? f[i + 2].pre : b.pre, p3, cur);
                double t2 = alpha * alpha;
                double t3 = t2 * alpha;
                for (int k = 0; k < 3; k++) {
                    out[k] = 0.5 * (2 * p1[k] + (-p0[k] + p2[k]) * alpha
                            + (2 * p0[k] - 5 * p1[k] + 4 * p2[k] - p3[k]) * t2
                            + (-p0[k] + 3 * p1[k] - 3 * p2[k] + p3[k]) * t3);
                }
            } else {
                for (int k = 0; k < 3; k++) out[k] = p1[k] + (p2[k] - p1[k]) * alpha;
            }
        }

        private static void eval(Env env, Script[] s, double[] out, double[] cur) {
            for (int i = 0; i < 3; i++) {
                env.thisValue = cur[i];
                out[i] = s[i].runNum(env);
            }
        }

        public boolean isStatic() {
            return constant != null;
        }
    }

    public static final class BoneAnim {
        public String bone;
        public Channel rotation;
        public Channel position;
        public Channel scale;
        public boolean rotationRelativeToEntity;
    }

    public static final class TimedEvent {
        public double time;
        public Script[] scripts = new Script[0];
        public String effect;
        public String locator;
        public String kind;
    }

    public String id;
    public double length;
    public Loop loop = Loop.NONE;
    public boolean overridePrevious;
    public Script animTimeUpdate;
    public Script blendWeight;
    public Script startDelay;
    public Script loopDelay;
    public final List<BoneAnim> bones = new ArrayList<>();
    public final List<TimedEvent> events = new ArrayList<>();

    public static Map<String, AnimationDef> parse(Object rootJson, Symbols symbols, List<String> problems) {
        Map<String, AnimationDef> out = new LinkedHashMap<>();
        Map<String, Object> root = Json.obj(rootJson);
        Map<String, Object> anims = root == null ? null : Json.obj(root.get("animations"));
        if (anims == null) return out;
        for (Map.Entry<String, Object> e : anims.entrySet()) {
            Map<String, Object> a = Json.obj(e.getValue());
            if (a == null) continue;
            try {
                AnimationDef def = parseOne(e.getKey(), a, symbols, problems);
                out.put(def.id.toLowerCase(Locale.ROOT), def);
            } catch (RuntimeException ex) {
                if (problems != null) problems.add("Animation " + e.getKey() + ": " + ex);
            }
        }
        return out;
    }

    private static AnimationDef parseOne(String id, Map<String, Object> a, Symbols symbols, List<String> problems) {
        AnimationDef d = new AnimationDef();
        d.id = id;
        d.length = Json.num(a.get("animation_length"), 0);
        Object loop = a.get("loop");
        if (loop instanceof Boolean b) d.loop = b ? Loop.LOOP : Loop.NONE;
        else if ("hold_on_last_frame".equals(loop)) d.loop = Loop.HOLD;
        d.overridePrevious = Json.bool(a.get("override_previous_animation"), false);
        d.animTimeUpdate = script(a.get("anim_time_update"), symbols, problems);
        d.blendWeight = script(a.get("blend_weight"), symbols, problems);
        d.startDelay = script(a.get("start_delay"), symbols, problems);
        d.loopDelay = script(a.get("loop_delay"), symbols, problems);
        Map<String, Object> bones = Json.obj(a.get("bones"));
        if (bones != null) {
            for (Map.Entry<String, Object> be : bones.entrySet()) {
                Map<String, Object> bj = Json.obj(be.getValue());
                if (bj == null) continue;
                BoneAnim ba = new BoneAnim();
                ba.bone = be.getKey();
                ba.rotation = channel(bj.get("rotation"), symbols, problems);
                ba.position = channel(bj.get("position"), symbols, problems);
                ba.scale = channel(bj.get("scale"), symbols, problems);
                Map<String, Object> rel = Json.obj(bj.get("relative_to"));
                ba.rotationRelativeToEntity = rel != null && "entity".equals(rel.get("rotation"));
                d.bones.add(ba);
            }
        }
        parseEvents(d, a.get("timeline"), "timeline", symbols, problems);
        parseEvents(d, a.get("sound_effects"), "sound", symbols, problems);
        parseEvents(d, a.get("particle_effects"), "particle", symbols, problems);
        d.events.sort(Comparator.comparingDouble(ev -> ev.time));
        if (d.length <= 0) {
            double max = 0;
            for (BoneAnim ba : d.bones) {
                for (Channel c : new Channel[] {ba.rotation, ba.position, ba.scale}) {
                    if (c != null && c.frames != null && c.frames.length > 0) {
                        max = Math.max(max, c.frames[c.frames.length - 1].time);
                    }
                }
            }
            d.length = max;
        }
        return d;
    }

    private static Script script(Object o, Symbols symbols, List<String> problems) {
        if (o == null) return null;
        if (o instanceof Double v) return Script.constant(v);
        if (o instanceof Boolean b) return Script.constant(b ? 1 : 0);
        if (o instanceof String s) return Script.compile(s, symbols, problems);
        return null;
    }

    private static Script[] triple(Object o, Symbols symbols, List<String> problems) {
        List<Object> l = Json.arr(o);
        Script[] out = new Script[3];
        if (l != null) {
            for (int i = 0; i < 3; i++) {
                Object c = i < l.size() ? l.get(i) : (l.size() == 1 ? l.get(0) : null);
                out[i] = c == null ? Script.constant(0) : script(c, symbols, problems);
                if (out[i] == null) out[i] = Script.constant(0);
            }
            return out;
        }
        Script s = script(o, symbols, problems);
        if (s == null) s = Script.constant(0);
        return new Script[] {s, s, s};
    }

    private static Channel channel(Object o, Symbols symbols, List<String> problems) {
        if (o == null) return null;
        Channel c = new Channel();
        Map<String, Object> m = Json.obj(o);
        if (m == null) {
            c.constant = triple(o, symbols, problems);
            return c;
        }
        List<Keyframe> frames = new ArrayList<>();
        for (Map.Entry<String, Object> e : m.entrySet()) {
            double time;
            try {
                time = Double.parseDouble(e.getKey());
            } catch (NumberFormatException ex) {
                continue;
            }
            Keyframe k = new Keyframe();
            k.time = time;
            Object v = e.getValue();
            Map<String, Object> vm = Json.obj(v);
            if (vm == null) {
                k.pre = k.post = triple(v, symbols, problems);
            } else {
                Object pre = vm.containsKey("pre") ? vm.get("pre") : vm.get("vector");
                Object post = vm.containsKey("post") ? vm.get("post") : vm.get("vector");
                if (pre == null) pre = post;
                if (post == null) post = pre;
                k.pre = triple(pre, symbols, problems);
                k.post = pre == post ? k.pre : triple(post, symbols, problems);
                String mode = Json.str(vm.get("lerp_mode"), "linear");
                k.lerp = mode.equals("catmullrom") ? CATMULL : mode.equals("step") ? STEP : LINEAR;
            }
            frames.add(k);
        }
        Keyframe[] arr = frames.toArray(new Keyframe[0]);
        Arrays.sort(arr, Comparator.comparingDouble(k -> k.time));
        c.frames = arr;
        return c;
    }

    private static void parseEvents(AnimationDef d, Object o, String kind, Symbols symbols, List<String> problems) {
        Map<String, Object> m = Json.obj(o);
        if (m == null) return;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            double time;
            try {
                time = Double.parseDouble(e.getKey());
            } catch (NumberFormatException ex) {
                continue;
            }
            Object v = e.getValue();
            List<Object> list = Json.arr(v);
            if (list == null) list = List.of(v);
            for (Object item : list) {
                TimedEvent ev = new TimedEvent();
                ev.time = time;
                ev.kind = kind;
                if (item instanceof String s) {
                    if (kind.equals("timeline")) ev.scripts = new Script[] {Script.compile(s, symbols, problems)};
                    else ev.effect = s;
                } else if (Json.obj(item) != null) {
                    Map<String, Object> im = Json.obj(item);
                    ev.effect = Json.str(im.get("effect"), null);
                    ev.locator = Json.str(im.get("locator"), null);
                    String pre = Json.str(im.get("pre_effect_script"), null);
                    if (pre != null) ev.scripts = new Script[] {Script.compile(pre, symbols, problems)};
                }
                d.events.add(ev);
            }
        }
    }
}
