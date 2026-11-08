package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.molang.Env;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Values;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EntityAnimator {

    private static final class AnimInstance {
        AnimationDef def;
        double time;
        boolean started;
        boolean finished;
        boolean touched;
        double delay;
        double loopWait;
        double lastEventTime = -1;
    }

    private static final class ControllerInstance {
        ControllerDef def;
        ControllerDef.State state;
        ControllerDef.State prevState;
        double stateTime;
        double blendElapsed;
        boolean anyFinished;
        boolean allFinished;
        String key;
        String prevKey;
        long ranSeq = -1;
    }

    public final ClientEntityDef def;
    public final Env env;
    public final Pose pose = new Pose();
    public final List<String> pendingSounds = new ArrayList<>();
    public final List<ParticleRequest> pendingParticles = new ArrayList<>();

    public record ParticleRequest(String particleId, String locator, Script preEffect) {}
    private final Map<String, AnimInstance> anims = new HashMap<>();
    private final Map<String, ControllerInstance> controllers = new HashMap<>();
    private boolean initialized;
    private long updateSeq;
    private int controllerDepth;
    private final Map<String, Integer> engineVarIds = new HashMap<>();
    private final double[] sampled = new double[3];
    private final double[] current = new double[3];

    public EntityAnimator(ClientEntityDef def) {
        this.def = def;
        this.env = new Env(def.pack.symbols);
        this.env.settings = def.pack.settings;
        this.env.entity.identifier = def.identifier;
    }

    public void update(double dt, double lifeTime, double frameAlpha) {
        updateSeq++;
        env.deltaTime = dt;
        env.lifeTime = lifeTime;
        env.frameAlpha = frameAlpha;
        env.timeStamp = lifeTime;
        seedEngineVars();
        if (!initialized) {
            initialized = true;
            if (def.initialize.length > 0) Script.runAll(env, def.initialize);
        }
        seedEngineVars();
        if (def.preAnimation.length > 0) Script.runAll(env, def.preAnimation);
        for (AnimInstance a : anims.values()) a.touched = false;
        pose.reset();
        for (ClientEntityDef.Conditional c : def.animate) {
            if (c.condition() != null && !Values.truthy(c.condition().run(env))) continue;
            String id = def.animations.get(c.name().toLowerCase(Locale.ROOT));
            if (id == null) continue;
            String lower = id.toLowerCase(Locale.ROOT);
            if (lower.startsWith("controller.")) {
                ControllerDef cd = def.pack.controller(lower);
                if (cd != null) runController(controllerFor(lower, cd), 1.0);
            } else {
                AnimationDef ad = def.pack.animation(lower);
                if (ad != null) play(instance(lower, ad), 1.0);
            }
        }
        for (AnimInstance a : anims.values()) {
            if (!a.touched) {
                a.started = false;
                a.time = 0;
                a.finished = false;
                a.lastEventTime = -1;
            }
        }
    }

    private void seedEngineVars() {
        Map<String, Double> vars = env.entity.engineVars;
        if (vars.isEmpty()) return;
        for (Map.Entry<String, Double> e : vars.entrySet()) {
            Integer id = engineVarIds.get(e.getKey());
            if (id == null) {
                id = env.symbols.varId(e.getKey());
                engineVarIds.put(e.getKey(), id);
            }
            env.setVar(id, e.getValue());
        }
    }

    private AnimInstance instance(String key, AnimationDef d) {
        AnimInstance a = anims.get(key);
        if (a == null) {
            a = new AnimInstance();
            a.def = d;
            anims.put(key, a);
        }
        return a;
    }

    private ControllerInstance controllerFor(String key, ControllerDef cd) {
        return controllers.computeIfAbsent(key, k -> {
            ControllerInstance c = new ControllerInstance();
            c.def = cd;
            c.key = k;
            return c;
        });
    }

    private void runController(ControllerInstance c, double weight) {
        if (c.ranSeq == updateSeq) return;
        if (controllerDepth >= 8) return;
        c.ranSeq = updateSeq;
        controllerDepth++;
        try {
            runControllerInner(c, weight);
        } finally {
            controllerDepth--;
        }
    }

    private void runControllerInner(ControllerInstance c, double weight) {
        if (c.state == null) {
            c.state = c.def.states.get(c.def.initialState);
            if (c.state == null && !c.def.states.isEmpty()) c.state = c.def.states.values().iterator().next();
            if (c.state == null) return;
            c.stateTime = 0;
            if (c.state.onEntry.length > 0) Script.runAll(env, c.state.onEntry);
            queueSounds(c.state.soundEffects);
            queueParticles(c.state.particleEffects);
        } else {
            c.stateTime += env.deltaTime;
            if (c.prevState != null) c.blendElapsed += env.deltaTime;
        }
        env.stateTime = c.stateTime;
        env.anyAnimationFinished = c.anyFinished;
        env.allAnimationsFinished = c.allFinished;
        for (ControllerDef.Transition t : c.state.transitions) {
            if (!Values.truthy(t.condition().run(env))) continue;
            ControllerDef.State next = c.def.states.get(t.target());
            if (next == null || next == c.state) break;
            if (c.state.onExit.length > 0) Script.runAll(env, c.state.onExit);
            c.prevState = c.state.blendDuration() > 0 ? c.state : null;
            c.blendElapsed = 0;
            c.state = next;
            c.stateTime = 0;
            c.anyFinished = false;
            c.allFinished = false;
            env.stateTime = 0;
            if (next.onEntry.length > 0) Script.runAll(env, next.onEntry);
            queueSounds(next.soundEffects);
            queueParticles(next.particleEffects);
            break;
        }
        double oldWeight = 0;
        if (c.prevState != null) {
            if (c.blendElapsed >= c.prevState.blendDuration()) {
                c.prevState = null;
            } else {
                oldWeight = c.prevState.blendWeightAt(c.blendElapsed);
            }
        }
        if (c.prevState != null) playState(c, c.prevState, weight * oldWeight, false);
        playState(c, c.state, weight * (1 - oldWeight), true);
    }

    private void playState(ControllerInstance c, ControllerDef.State s, double weight, boolean current) {
        boolean any = false;
        boolean all = true;
        boolean has = false;
        env.stateTime = current ? c.stateTime : c.stateTime;
        for (ControllerDef.AnimRef ref : s.animations) {
            String id = def.animations.get(ref.name().toLowerCase(Locale.ROOT));
            if (id == null) continue;
            String lowerId = id.toLowerCase(Locale.ROOT);
            if (lowerId.startsWith("controller.")) {
                ControllerDef nested = def.pack.controller(lowerId);
                if (nested == null) continue;
                double nw = weight;
                if (ref.blend() != null) nw *= ref.blend().runNum(env);
                double savedStateTime = env.stateTime;
                runController(controllerFor(lowerId, nested), nw);
                env.stateTime = savedStateTime;
                has = true;
                continue;
            }
            AnimationDef ad = def.pack.animation(lowerId);
            if (ad == null) continue;
            double w = weight;
            if (ref.blend() != null) w *= ref.blend().runNum(env);
            AnimInstance ai = instance(c.key + "|" + s.name + "|" + id.toLowerCase(Locale.ROOT), ad);
            play(ai, w);
            has = true;
            any |= ai.finished;
            all &= ai.finished;
        }
        if (current) {
            c.anyFinished = any;
            c.allFinished = has && all;
        }
    }

    private void play(AnimInstance ai, double weight) {
        AnimationDef d = ai.def;
        ai.touched = true;
        if (!ai.started) {
            ai.started = true;
            ai.time = 0;
            ai.finished = false;
            ai.lastEventTime = -1;
            ai.loopWait = 0;
            ai.delay = d.startDelay == null ? 0 : Math.max(0, d.startDelay.runNum(env));
        } else if (ai.delay > 0) {
            ai.delay -= env.deltaTime;
        } else if (ai.loopWait > 0) {
            ai.loopWait -= env.deltaTime;
        } else if (d.animTimeUpdate != null) {
            env.animTime = ai.time;
            ai.time = d.animTimeUpdate.runNum(env);
        } else {
            ai.time += env.deltaTime;
        }
        if (ai.delay > 0) return;
        double length = d.length;
        switch (d.loop) {
            case LOOP -> {
                if (length > 0 && ai.time >= length) {
                    ai.time %= length;
                    ai.lastEventTime = -1;
                    if (d.loopDelay != null) {
                        ai.loopWait = Math.max(0, d.loopDelay.runNum(env));
                        if (ai.loopWait > 0) ai.time = 0;
                    }
                }
            }
            case HOLD -> {
                if (ai.time >= length) {
                    ai.time = length;
                    ai.finished = true;
                }
            }
            default -> {
                if (ai.time >= length) ai.finished = true;
            }
        }
        env.animTime = ai.time;
        fireEvents(ai);
        if (d.loop == AnimationDef.Loop.NONE && ai.finished && length > 0 && ai.time > length) return;
        double w = weight;
        if (d.blendWeight != null) w *= d.blendWeight.runNum(env);
        if (w == 0) return;
        if (d.overridePrevious) pose.reset();
        double t = ai.time;
        for (AnimationDef.BoneAnim ba : d.bones) {
            Pose.Xf xf = pose.get(ba.bone);
            if (ba.rotation != null) {
                if (ba.rotationRelativeToEntity) xf.entityRotation = true;
                current[0] = -Math.toDegrees(xf.rx);
                current[1] = -Math.toDegrees(xf.ry);
                current[2] = Math.toDegrees(xf.rz);
                ba.rotation.sample(env, t, sampled, current);
                xf.rx += Math.toRadians(-sampled[0]) * w;
                xf.ry += Math.toRadians(-sampled[1]) * w;
                xf.rz += Math.toRadians(sampled[2]) * w;
            }
            if (ba.position != null) {
                current[0] = -xf.px;
                current[1] = xf.py;
                current[2] = xf.pz;
                ba.position.sample(env, t, sampled, current);
                xf.px += -sampled[0] * w;
                xf.py += sampled[1] * w;
                xf.pz += sampled[2] * w;
            }
            if (ba.scale != null) {
                current[0] = xf.sx;
                current[1] = xf.sy;
                current[2] = xf.sz;
                ba.scale.sample(env, t, sampled, current);
                xf.sx *= 1 + (sampled[0] - 1) * w;
                xf.sy *= 1 + (sampled[1] - 1) * w;
                xf.sz *= 1 + (sampled[2] - 1) * w;
            }
        }
    }

    private void fireEvents(AnimInstance ai) {
        List<AnimationDef.TimedEvent> events = ai.def.events;
        if (events.isEmpty()) return;
        double now = ai.time;
        for (AnimationDef.TimedEvent ev : events) {
            if (ev.time > ai.lastEventTime && ev.time <= now) {
                if (ev.kind.equals("particle")) {
                    if (ev.effect != null) queueParticle(ev.effect, ev.locator, ev.scripts.length > 0 ? ev.scripts[0] : null);
                    continue;
                }
                if (ev.scripts.length > 0) Script.runAll(env, ev.scripts);
                if (ev.effect != null && ev.kind.equals("sound")) queueSound(ev.effect);
            }
        }
        ai.lastEventTime = now;
    }

    private void queueParticles(List<ControllerDef.ParticleRef> refs) {
        for (ControllerDef.ParticleRef r : refs) queueParticle(r.effect(), r.locator(), null);
    }

    private void queueParticle(String shortName, String locator, Script pre) {
        String id = def.particleEffects.get(shortName.toLowerCase(Locale.ROOT));
        if (id != null) pendingParticles.add(new ParticleRequest(id.toLowerCase(Locale.ROOT), locator, pre));
    }

    private void queueSounds(List<String> names) {
        for (String n : names) queueSound(n);
    }

    private void queueSound(String shortName) {
        String event = def.soundEffects.get(shortName.toLowerCase(Locale.ROOT));
        if (event != null) pendingSounds.add(event);
    }

    public double entityScale() {
        return def.scale == null ? 1 : def.scale.runNum(env);
    }

    public int activeAnimations() {
        int n = 0;
        for (AnimInstance a : anims.values()) if (a.touched) n++;
        return n;
    }
}
