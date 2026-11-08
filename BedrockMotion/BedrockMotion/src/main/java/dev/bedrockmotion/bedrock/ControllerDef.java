package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Symbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ControllerDef {

    public record AnimRef(String name, Script blend) {}

    public record Transition(String target, Script condition) {}

    public record ParticleRef(String effect, String locator) {}

    public static final class State {
        public String name;
        public final List<AnimRef> animations = new ArrayList<>();
        public final List<Transition> transitions = new ArrayList<>();
        public Script[] onEntry = new Script[0];
        public Script[] onExit = new Script[0];
        public final List<String> soundEffects = new ArrayList<>();
        public final List<ParticleRef> particleEffects = new ArrayList<>();
        public double[] blendTimes;
        public double[] blendWeights;
        public boolean blendShortestPath;

        public double blendWeightAt(double elapsed) {
            if (blendTimes == null || blendTimes.length == 0) return 0;
            if (elapsed <= blendTimes[0]) return blendWeights[0];
            int n = blendTimes.length;
            if (elapsed >= blendTimes[n - 1]) return blendWeights[n - 1];
            for (int i = 0; i + 1 < n; i++) {
                if (elapsed < blendTimes[i + 1]) {
                    double span = blendTimes[i + 1] - blendTimes[i];
                    double a = span <= 0 ? 1 : (elapsed - blendTimes[i]) / span;
                    return blendWeights[i] + (blendWeights[i + 1] - blendWeights[i]) * a;
                }
            }
            return 0;
        }

        public double blendDuration() {
            return blendTimes == null || blendTimes.length == 0 ? 0 : blendTimes[blendTimes.length - 1];
        }
    }

    public String id;
    public String initialState = "default";
    public final Map<String, State> states = new LinkedHashMap<>();

    public static Map<String, ControllerDef> parse(Object rootJson, Symbols symbols, List<String> problems) {
        Map<String, ControllerDef> out = new LinkedHashMap<>();
        Map<String, Object> root = Json.obj(rootJson);
        Map<String, Object> ctrls = root == null ? null : Json.obj(root.get("animation_controllers"));
        if (ctrls == null) return out;
        for (Map.Entry<String, Object> e : ctrls.entrySet()) {
            Map<String, Object> c = Json.obj(e.getValue());
            if (c == null) continue;
            try {
                ControllerDef d = new ControllerDef();
                d.id = e.getKey();
                d.initialState = Json.str(c.get("initial_state"), "default");
                Map<String, Object> states = Json.obj(c.get("states"));
                if (states != null) {
                    for (Map.Entry<String, Object> se : states.entrySet()) {
                        Map<String, Object> sj = Json.obj(se.getValue());
                        if (sj != null) d.states.put(se.getKey(), state(se.getKey(), sj, symbols, problems));
                    }
                }
                out.put(d.id.toLowerCase(Locale.ROOT), d);
            } catch (RuntimeException ex) {
                if (problems != null) problems.add("Controller " + e.getKey() + ": " + ex);
            }
        }
        return out;
    }

    private static State state(String name, Map<String, Object> j, Symbols symbols, List<String> problems) {
        State s = new State();
        s.name = name;
        List<Object> anims = Json.arr(j.get("animations"));
        if (anims != null) {
            for (Object a : anims) {
                if (a instanceof String n) {
                    s.animations.add(new AnimRef(n, null));
                } else if (Json.obj(a) != null) {
                    for (Map.Entry<String, Object> e : Json.obj(a).entrySet()) {
                        Script blend = e.getValue() instanceof String x ? Script.compile(x, symbols, problems)
                                : e.getValue() instanceof Double dv ? Script.constant(dv) : null;
                        s.animations.add(new AnimRef(e.getKey(), blend));
                    }
                }
            }
        }
        List<Object> trans = Json.arr(j.get("transitions"));
        if (trans != null) {
            for (Object t : trans) {
                Map<String, Object> tm = Json.obj(t);
                if (tm == null) continue;
                for (Map.Entry<String, Object> e : tm.entrySet()) {
                    String cond = e.getValue() instanceof String x ? x : e.getValue() instanceof Double dv ? dv.toString() : "0";
                    s.transitions.add(new Transition(e.getKey(), Script.compile(cond, symbols, problems)));
                }
            }
        }
        s.onEntry = scripts(j.get("on_entry"), symbols, problems);
        s.onExit = scripts(j.get("on_exit"), symbols, problems);
        Object bt = j.get("blend_transition");
        if (bt instanceof Double secs && secs > 0) {
            s.blendTimes = new double[] {0, secs};
            s.blendWeights = new double[] {1, 0};
        } else if (Json.obj(bt) != null) {
            Map<String, Object> m = Json.obj(bt);
            List<double[]> pts = new ArrayList<>();
            for (Map.Entry<String, Object> e : m.entrySet()) {
                try {
                    pts.add(new double[] {Double.parseDouble(e.getKey()), Json.num(e.getValue(), 0)});
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
            pts.sort((a, b) -> Double.compare(a[0], b[0]));
            if (!pts.isEmpty()) {
                s.blendTimes = new double[pts.size()];
                s.blendWeights = new double[pts.size()];
                for (int i = 0; i < pts.size(); i++) {
                    s.blendTimes[i] = pts.get(i)[0];
                    s.blendWeights[i] = pts.get(i)[1];
                }
            }
        }
        List<Object> sounds = Json.arr(j.get("sound_effects"));
        if (sounds != null) {
            for (Object o : sounds) {
                Map<String, Object> m = Json.obj(o);
                if (m != null && m.get("effect") instanceof String effect) s.soundEffects.add(effect);
            }
        }
        List<Object> particles = Json.arr(j.get("particle_effects"));
        if (particles != null) {
            for (Object o : particles) {
                Map<String, Object> m = Json.obj(o);
                if (m != null && m.get("effect") instanceof String effect) {
                    s.particleEffects.add(new ParticleRef(effect, Json.str(m.get("locator"), null)));
                }
            }
        }
        s.blendShortestPath = Json.bool(j.get("blend_via_shortest_path"), false);
        return s;
    }

    private static Script[] scripts(Object o, Symbols symbols, List<String> problems) {
        List<Object> l = Json.arr(o);
        if (l == null) return new Script[0];
        List<Script> out = new ArrayList<>();
        for (Object x : l) if (x instanceof String s) out.add(Script.compile(s, symbols, problems));
        return out.toArray(new Script[0]);
    }
}
