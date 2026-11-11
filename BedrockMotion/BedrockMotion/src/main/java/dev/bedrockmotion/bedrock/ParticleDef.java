package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Symbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class ParticleDef {

    public enum Rate { INSTANT, STEADY, MANUAL }

    public enum Life { ONCE, LOOPING, EXPRESSION }

    public enum Shape { POINT, SPHERE, BOX, DISC, CUSTOM }

    public static final int FACE_CAMERA = 0, FACE_Y = 1, FACE_XY = 2, FACE_XZ = 3, FACE_YZ = 4, FACE_DIRECTION = 5;

    public static final class Curve {
        public int varId;
        public Script input;
        public Script range;
        public double[] nodes;
        public boolean catmull;
    }

    public static final class EventNode {
        public Script expression;
        public String effect;
        public String effectType = "emitter";
        public Script preEffect;
        public String sound;
        public List<EventNode> sequence;
        public List<EventNode> randomize;
        public List<Double> weights;
    }

    public String id;
    public String material = "particles_alpha";
    public static final String ATLAS_TERRAIN = "atlas.terrain";

    public String texture;
    public Symbols symbols;
    public final List<Curve> curves = new ArrayList<>();
    public Script[] emitterCreation = new Script[0];
    public Script[] emitterPerUpdate = new Script[0];
    public Script[] particlePerUpdate = new Script[0];
    public Script[] particlePerRender = new Script[0];
    public Rate rate = Rate.INSTANT;
    public Script numParticles;
    public Script spawnRate;
    public Script maxParticles;
    public Life life = Life.ONCE;
    public Script activeTime;
    public Script sleepTime;
    public Script activation;
    public Script expiration;
    public Shape shape = Shape.POINT;
    public Script[] shapeOffset;
    public Script[] halfDimensions;
    public Script radius;
    public Script[] planeNormal;
    public Script[] direction;
    public boolean directionInwards;
    public boolean surfaceOnly;
    public boolean localPosition;
    public boolean localRotation;
    public boolean localVelocity;
    public Script maxLifetime;
    public Script particleExpiration;
    public Script initialSpin;
    public Script initialSpinRate;
    public Script speed;
    public Script[] speedVector;
    public boolean dynamic;
    public Script[] acceleration;
    public Script drag;
    public Script rotationAcceleration;
    public Script rotationDrag;
    public Script[] parametricPosition;
    public Script parametricRotation;
    public Script[] size;
    public int facing = FACE_CAMERA;
    public double texWidth = 16;
    public double texHeight = 16;
    public Script[] uv;
    public Script[] uvSize;
    public boolean flipbook;
    public Script[] baseUv;
    public Script[] stepUv;
    public Script fps;
    public Script maxFrame;
    public boolean stretchToLifetime;
    public boolean loopFrames;
    public Script[] color;
    public Script gradientInterpolant;
    public TreeMap<Double, float[]> gradient;
    public final Map<String, EventNode> events = new LinkedHashMap<>();
    public final List<String> emitterCreationEvents = new ArrayList<>();
    public final List<String> emitterExpirationEvents = new ArrayList<>();
    public final TreeMap<Double, List<String>> emitterTimeline = new TreeMap<>();
    public final List<String> particleCreationEvents = new ArrayList<>();
    public final List<String> particleExpirationEvents = new ArrayList<>();
    public final TreeMap<Double, List<String>> particleTimeline = new TreeMap<>();
    public int vParticleAge, vParticleLifetime, vEmitterAge, vEmitterLifetime;
    public final int[] vParticleRandom = new int[4];
    public final int[] vEmitterRandom = new int[4];

    public static ParticleDef parse(Object rootJson, Symbols symbols, List<String> problems) {
        Map<String, Object> root = Json.obj(rootJson);
        Map<String, Object> effect = root == null ? null : Json.obj(root.get("particle_effect"));
        Map<String, Object> desc = effect == null ? null : Json.obj(effect.get("description"));
        if (desc == null || !(desc.get("identifier") instanceof String id)) return null;
        ParticleDef d = new ParticleDef();
        d.id = id.toLowerCase(Locale.ROOT);
        d.symbols = symbols;
        Map<String, Object> render = Json.obj(desc.get("basic_render_parameters"));
        if (render != null) {
            d.material = Json.str(render.get("material"), d.material);
            d.texture = Json.str(render.get("texture"), null);
        }
        d.vParticleAge = symbols.varId("particle_age");
        d.vParticleLifetime = symbols.varId("particle_lifetime");
        d.vEmitterAge = symbols.varId("emitter_age");
        d.vEmitterLifetime = symbols.varId("emitter_lifetime");
        for (int i = 0; i < 4; i++) {
            d.vParticleRandom[i] = symbols.varId("particle_random_" + (i + 1));
            d.vEmitterRandom[i] = symbols.varId("emitter_random_" + (i + 1));
        }
        d.parseCurves(Json.obj(effect.get("curves")), problems);
        Map<String, Object> comps = Json.obj(effect.get("components"));
        if (comps != null) d.parseComponents(comps, problems);
        Map<String, Object> events = Json.obj(effect.get("events"));
        if (events != null) {
            for (Map.Entry<String, Object> e : events.entrySet()) {
                d.events.put(e.getKey(), d.parseEvent(e.getValue(), problems, 0));
            }
        }
        return d;
    }

    private Script num(Object o, double def, List<String> problems) {
        if (o instanceof Double v) return Script.constant(v);
        if (o instanceof Boolean b) return Script.constant(b ? 1 : 0);
        if (o instanceof String s) return Script.compile(s, symbols, problems);
        return Script.constant(def);
    }

    private Script numOrNull(Object o, List<String> problems) {
        return o == null ? null : num(o, 0, problems);
    }

    private Script[] vec(Object o, int n, double def, List<String> problems) {
        List<Object> l = Json.arr(o);
        if (l == null) return null;
        Script[] out = new Script[n];
        for (int i = 0; i < n; i++) out[i] = num(i < l.size() ? l.get(i) : null, def, problems);
        return out;
    }

    private Script[] scripts(Object o, List<String> problems) {
        List<Script> out = new ArrayList<>();
        if (o instanceof String s) out.add(Script.compile(s, symbols, problems));
        List<Object> l = Json.arr(o);
        if (l != null) for (Object x : l) if (x instanceof String s) out.add(Script.compile(s, symbols, problems));
        return out.toArray(new Script[0]);
    }

    private String varName(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        if (k.startsWith("variable.")) return k.substring(9);
        if (k.startsWith("v.")) return k.substring(2);
        return k;
    }

    private void parseCurves(Map<String, Object> curves, List<String> problems) {
        if (curves == null) return;
        for (Map.Entry<String, Object> e : curves.entrySet()) {
            Map<String, Object> c = Json.obj(e.getValue());
            List<Object> nodes = c == null ? null : Json.arr(c.get("nodes"));
            if (nodes == null) continue;
            Curve curve = new Curve();
            curve.varId = symbols.varId(varName(e.getKey()));
            curve.input = num(c.get("input"), 0, problems);
            curve.range = num(c.get("horizontal_range"), 1, problems);
            List<Double> values = new ArrayList<>();
            for (Object n : nodes) {
                if (n instanceof Double v) values.add(v);
            }
            if (values.size() < 2) continue;
            curve.nodes = values.stream().mapToDouble(Double::doubleValue).toArray();
            curve.catmull = "catmull_rom".equals(Json.str(c.get("type"), "linear"));
            this.curves.add(curve);
        }
    }

    private void parseComponents(Map<String, Object> comps, List<String> problems) {
        Map<String, Object> m;
        if ((m = comp(comps, "emitter_initialization")) != null) {
            emitterCreation = scripts(m.get("creation_expression"), problems);
            emitterPerUpdate = scripts(m.get("per_update_expression"), problems);
        }
        if ((m = comp(comps, "particle_initialization")) != null) {
            particlePerUpdate = scripts(m.get("per_update_expression"), problems);
            particlePerRender = scripts(m.get("per_render_expression"), problems);
        }
        if ((m = comp(comps, "emitter_rate_instant")) != null) {
            rate = Rate.INSTANT;
            numParticles = num(m.get("num_particles"), 10, problems);
        } else if ((m = comp(comps, "emitter_rate_steady")) != null) {
            rate = Rate.STEADY;
            spawnRate = num(m.get("spawn_rate"), 1, problems);
            maxParticles = num(m.get("max_particles"), 50, problems);
        } else if ((m = comp(comps, "emitter_rate_manual")) != null) {
            rate = Rate.MANUAL;
            maxParticles = num(m.get("max_particles"), 50, problems);
        } else {
            numParticles = Script.constant(10);
        }
        if ((m = comp(comps, "emitter_lifetime_looping")) != null) {
            life = Life.LOOPING;
            activeTime = num(m.get("active_time"), 10, problems);
            sleepTime = num(m.get("sleep_time"), 0, problems);
        } else if ((m = comp(comps, "emitter_lifetime_expression")) != null) {
            life = Life.EXPRESSION;
            activation = num(m.get("activation_expression"), 1, problems);
            expiration = num(m.get("expiration_expression"), 0, problems);
        } else {
            life = Life.ONCE;
            m = comp(comps, "emitter_lifetime_once");
            activeTime = num(m == null ? null : m.get("active_time"), 10, problems);
        }
        parseShape(comps, problems);
        if ((m = comp(comps, "emitter_local_space")) != null) {
            localPosition = Json.bool(m.get("position"), false);
            localRotation = Json.bool(m.get("rotation"), false);
            localVelocity = Json.bool(m.get("velocity"), false);
        }
        if ((m = comp(comps, "particle_lifetime_expression")) != null) {
            maxLifetime = numOrNull(m.get("max_lifetime"), problems);
            particleExpiration = numOrNull(m.get("expiration_expression"), problems);
        }
        if (maxLifetime == null) maxLifetime = Script.constant(1);
        if ((m = comp(comps, "particle_initial_spin")) != null) {
            initialSpin = numOrNull(m.get("rotation"), problems);
            initialSpinRate = numOrNull(m.get("rotation_rate"), problems);
        }
        Object speedObj = comps.containsKey("minecraft:particle_initial_speed") ? comps.get("minecraft:particle_initial_speed") : null;
        if (speedObj != null) {
            speedVector = vec(speedObj, 3, 0, problems);
            if (speedVector == null) speed = num(speedObj, 0, problems);
        }
        if ((m = comp(comps, "particle_motion_dynamic")) != null) {
            dynamic = true;
            acceleration = vec(m.get("linear_acceleration"), 3, 0, problems);
            drag = numOrNull(m.get("linear_drag_coefficient"), problems);
            rotationAcceleration = numOrNull(m.get("rotation_acceleration"), problems);
            rotationDrag = numOrNull(m.get("rotation_drag_coefficient"), problems);
        }
        if ((m = comp(comps, "particle_motion_parametric")) != null) {
            parametricPosition = vec(m.get("relative_position"), 3, 0, problems);
            parametricRotation = numOrNull(m.get("rotation"), problems);
        }
        if ((m = comp(comps, "particle_appearance_billboard")) != null) parseBillboard(m, problems);
        if ((m = comp(comps, "particle_appearance_tinting")) != null) parseTint(m.get("color"), problems);
        if ((m = comp(comps, "emitter_lifetime_events")) != null) {
            names(m.get("creation_event"), emitterCreationEvents);
            names(m.get("expiration_event"), emitterExpirationEvents);
            timeline(m.get("timeline"), emitterTimeline);
        }
        if ((m = comp(comps, "particle_lifetime_events")) != null) {
            names(m.get("creation_event"), particleCreationEvents);
            names(m.get("expiration_event"), particleExpirationEvents);
            timeline(m.get("timeline"), particleTimeline);
        }
    }

    private static Map<String, Object> comp(Map<String, Object> comps, String name) {
        return Json.obj(comps.get("minecraft:" + name));
    }

    private void parseShape(Map<String, Object> comps, List<String> problems) {
        Map<String, Object> m;
        if ((m = comp(comps, "emitter_shape_sphere")) != null) shape = Shape.SPHERE;
        else if ((m = comp(comps, "emitter_shape_box")) != null) shape = Shape.BOX;
        else if ((m = comp(comps, "emitter_shape_disc")) != null) shape = Shape.DISC;
        else if ((m = comp(comps, "emitter_shape_custom")) != null) shape = Shape.CUSTOM;
        else if ((m = comp(comps, "emitter_shape_point")) != null) shape = Shape.POINT;
        if (m == null) return;
        shapeOffset = vec(m.get("offset"), 3, 0, problems);
        halfDimensions = vec(m.get("half_dimensions"), 3, 0, problems);
        radius = numOrNull(m.get("radius"), problems);
        surfaceOnly = Json.bool(m.get("surface_only"), false);
        Object normal = m.get("plane_normal");
        if (normal instanceof String s) {
            planeNormal = switch (s.toLowerCase(Locale.ROOT)) {
                case "x" -> new Script[] {Script.constant(1), Script.constant(0), Script.constant(0)};
                case "z" -> new Script[] {Script.constant(0), Script.constant(0), Script.constant(1)};
                default -> new Script[] {Script.constant(0), Script.constant(1), Script.constant(0)};
            };
        } else {
            planeNormal = vec(normal, 3, 0, problems);
        }
        Object dir = m.get("direction");
        if (dir instanceof String s) directionInwards = s.equalsIgnoreCase("inwards");
        else direction = vec(dir, 3, 0, problems);
    }

    private void parseBillboard(Map<String, Object> m, List<String> problems) {
        size = vec(m.get("size"), 2, 0.1, problems);
        String mode = Json.str(m.get("facing_camera_mode"), "rotate_xyz").toLowerCase(Locale.ROOT);
        facing = switch (mode) {
            case "rotate_y", "lookat_y" -> FACE_Y;
            case "emitter_transform_xy" -> FACE_XY;
            case "emitter_transform_xz" -> FACE_XZ;
            case "emitter_transform_yz" -> FACE_YZ;
            case "direction_x", "direction_y", "direction_z", "lookat_direction" -> FACE_DIRECTION;
            default -> FACE_CAMERA;
        };
        Map<String, Object> u = Json.obj(m.get("uv"));
        if (u == null) return;
        double unit = ATLAS_TERRAIN.equalsIgnoreCase(texture) ? 1 : 16;
        texWidth = Json.num(u.get("texture_width"), unit);
        texHeight = Json.num(u.get("texture_height"), unit);
        Map<String, Object> fb = Json.obj(u.get("flipbook"));
        if (fb != null) {
            flipbook = true;
            baseUv = vec(fb.get("base_UV"), 2, 0, problems);
            uvSize = vec(fb.get("size_UV"), 2, unit, problems);
            stepUv = vec(fb.get("step_UV"), 2, 0, problems);
            fps = numOrNull(fb.get("frames_per_second"), problems);
            maxFrame = num(fb.get("max_frame"), 1, problems);
            stretchToLifetime = Json.bool(fb.get("stretch_to_lifetime"), false);
            loopFrames = Json.bool(fb.get("loop"), false);
        } else {
            uv = vec(u.get("uv"), 2, 0, problems);
            uvSize = vec(u.get("uv_size"), 2, unit, problems);
        }
    }

    private void parseTint(Object c, List<String> problems) {
        if (c == null) return;
        Map<String, Object> g = Json.obj(c);
        if (g == null) {
            color = vec(c, 4, 1, problems);
            return;
        }
        gradientInterpolant = num(g.get("interpolant"), 0, problems);
        TreeMap<Double, float[]> stops = new TreeMap<>();
        Map<String, Object> gm = Json.obj(g.get("gradient"));
        List<Object> gl = Json.arr(g.get("gradient"));
        if (gm != null) {
            for (Map.Entry<String, Object> e : gm.entrySet()) {
                try {
                    float[] rgba = hex(e.getValue());
                    if (rgba != null) stops.put(Double.parseDouble(e.getKey()), rgba);
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
        } else if (gl != null) {
            for (int i = 0; i < gl.size(); i++) {
                float[] rgba = hex(gl.get(i));
                if (rgba != null) stops.put(gl.size() == 1 ? 0.0 : i / (double) (gl.size() - 1), rgba);
            }
        }
        if (!stops.isEmpty()) gradient = stops;
    }

    private static float[] hex(Object o) {
        if (!(o instanceof String s) || !s.startsWith("#")) return null;
        String h = s.substring(1);
        try {
            long v = Long.parseLong(h, 16);
            if (h.length() == 6) return new float[] {((v >> 16) & 255) / 255f, ((v >> 8) & 255) / 255f, (v & 255) / 255f, 1f};
            if (h.length() == 8) {
                return new float[] {((v >> 16) & 255) / 255f, ((v >> 8) & 255) / 255f, (v & 255) / 255f, ((v >> 24) & 255) / 255f};
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private static void names(Object o, List<String> out) {
        if (o instanceof String s) out.add(s);
        List<Object> l = Json.arr(o);
        if (l != null) for (Object x : l) if (x instanceof String s) out.add(s);
    }

    private static void timeline(Object o, TreeMap<Double, List<String>> out) {
        Map<String, Object> m = Json.obj(o);
        if (m == null) return;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            try {
                List<String> list = new ArrayList<>();
                names(e.getValue(), list);
                out.put(Double.parseDouble(e.getKey()), list);
            } catch (NumberFormatException ignored) {
                continue;
            }
        }
    }

    private EventNode parseEvent(Object o, List<String> problems, int depth) {
        EventNode n = new EventNode();
        Map<String, Object> m = Json.obj(o);
        if (m == null || depth > 8) return n;
        if (m.get("expression") instanceof String s) n.expression = Script.compile(s, symbols, problems);
        Map<String, Object> pe = Json.obj(m.get("particle_effect"));
        if (pe != null) {
            n.effect = Json.str(pe.get("effect"), null);
            n.effectType = Json.str(pe.get("type"), "emitter");
            if (pe.get("pre_effect_expression") instanceof String s) n.preEffect = Script.compile(s, symbols, problems);
        }
        Map<String, Object> se = Json.obj(m.get("sound_effect"));
        if (se != null) n.sound = Json.str(se.get("event_name"), null);
        List<Object> seq = Json.arr(m.get("sequence"));
        if (seq != null) {
            n.sequence = new ArrayList<>();
            for (Object x : seq) n.sequence.add(parseEvent(x, problems, depth + 1));
        }
        List<Object> rnd = Json.arr(m.get("randomize"));
        if (rnd != null) {
            n.randomize = new ArrayList<>();
            n.weights = new ArrayList<>();
            for (Object x : rnd) {
                n.randomize.add(parseEvent(x, problems, depth + 1));
                n.weights.add(Math.max(0, Json.num(Json.obj(x) == null ? null : Json.obj(x).get("weight"), 1)));
            }
        }
        return n;
    }
}
