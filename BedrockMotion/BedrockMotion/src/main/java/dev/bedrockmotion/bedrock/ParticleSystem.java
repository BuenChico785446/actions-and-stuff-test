package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.molang.Env;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Values;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ParticleSystem {

    public static volatile double sizeFactor = 1.0;

    public interface Resolver {
        ParticleDef find(String id);
    }

    public static final class RenderParticle {
        public double x, y, z;
        public double vx, vy, vz;
        public float width, height, roll;
        public float u0, v0, u1, v1;
        public int argb;
    }

    public static final class EmitterView {
        public ParticleDef def;
        public final List<RenderParticle> particles = new ArrayList<>();
    }

    private static final class Particle {
        final double[] pos = new double[3];
        final double[] vel = new double[3];
        final double[] rnd = new double[4];
        Object[] vars;
        double age;
        double lifetime = 1;
        double roll;
        double rollRate;
        boolean dead;
        final Set<Double> fired = new HashSet<>();
    }

    private static final class Emitter {
        ParticleDef def;
        Env env;
        Object[] vars;
        double[] localPos = new double[3];
        boolean bound;
        final double[] fixedOrigin = new double[3];
        double age;
        double activeTime = 10;
        double sleepTime;
        double spawnAccumulator;
        boolean active;
        boolean wasActive;
        boolean dead;
        boolean stopped;
        double maxAge = Double.MAX_VALUE;
        int generation;
        final double[] rnd = new double[4];
        final List<Particle> particles = new ArrayList<>();
        final Set<Double> fired = new HashSet<>();
    }

    public final List<String> pendingSounds = new ArrayList<>();
    public int maxParticles = 1500;
    public int maxEmitters = 48;
    private final Resolver resolver;
    private final Env owner;
    private final Random random = new Random();
    private final List<Emitter> emitters = new ArrayList<>();
    private final List<Emitter> incoming = new ArrayList<>();
    private double[] entityPos = new double[3];
    private double yaw;

    public ParticleSystem(Resolver resolver, Env owner) {
        this.resolver = resolver;
        this.owner = owner;
    }

    public void updateDetached(double dt) {
        update(dt, entityPos, yaw);
    }

    public void detach() {
        for (Emitter e : emitters) {
            if (e.bound) {
                double[] o = origin(e);
                System.arraycopy(o, 0, e.fixedOrigin, 0, 3);
                e.bound = false;
            }
            e.stopped = true;
        }
    }

    public double[] lastPosition() {
        return entityPos.clone();
    }

    public void collectWorld(double[] base, List<EmitterView> out) {
        collect(base, yaw, out);
    }

    public boolean isEmpty() {
        return emitters.isEmpty();
    }

    public int particleCount() {
        int n = 0;
        for (Emitter e : emitters) n += e.particles.size();
        return n;
    }

    public void spawn(String effectId, double[] localPos, Script preEffect, double[] entityPosition, double yawDegrees) {
        ParticleDef def = resolver.find(effectId);
        if (def == null) return;
        entityPos = entityPosition;
        yaw = yawDegrees;
        if (emitters.size() >= maxEmitters) emitters.remove(0);
        Emitter e = create(def, localPos, true, null, 0, preEffect, owner, true);
        emitters.add(e);
        emitters.addAll(incoming);
        incoming.clear();
    }

    public void update(double dt, double[] entityPosition, double yawDegrees) {
        entityPos = entityPosition;
        yaw = yawDegrees;
        for (Emitter e : emitters) updateEmitter(e, dt);
        emitters.addAll(incoming);
        incoming.clear();
        emitters.removeIf(e -> e.dead);
        while (emitters.size() > maxEmitters) emitters.remove(0);
    }

    private double[] origin(Emitter e) {
        if (!e.bound) return e.fixedOrigin;
        double[] r = rotateY(e.localPos, 180.0 - yaw);
        return new double[] {entityPos[0] + r[0], entityPos[1] + r[1], entityPos[2] + r[2]};
    }

    private static double[] rotateY(double[] v, double degrees) {
        double a = Math.toRadians(degrees);
        double c = Math.cos(a), s = Math.sin(a);
        return new double[] {v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c};
    }

    private Emitter create(
            ParticleDef def, double[] localPos, boolean bound, double[] fixed, int generation,
            Script pre, Env preEnv, boolean freshScope) {
        Emitter e = new Emitter();
        e.def = def;
        e.generation = generation;
        e.bound = bound;
        e.localPos = localPos == null ? new double[3] : localPos.clone();
        e.env = new Env(def.symbols);
        e.env.entity = owner.entity;
        e.env.probe = owner.probe;
        e.env.settings = owner.settings;
        e.vars = new Object[Math.max(16, def.symbols.varCount())];
        for (int i = 0; i < 4; i++) e.rnd[i] = random.nextDouble();
        if (!bound) {
            double[] o = fixed != null ? fixed : originOfLocal(e.localPos);
            System.arraycopy(o, 0, e.fixedOrigin, 0, 3);
        }
        if (pre != null) {
            Object[] saved = preEnv.vars;
            preEnv.vars = e.vars;
            try {
                if (freshScope) pre.run(preEnv);
                else pre.exec(preEnv);
            } finally {
                e.vars = preEnv.vars;
                preEnv.vars = saved;
            }
        }
        Env env = e.env;
        env.vars = e.vars;
        setEmitterVars(e);
        if (def.emitterCreation.length > 0) Script.runAll(env, def.emitterCreation);
        applyCurves(e.def, env);
        switch (def.life) {
            case LOOPING -> {
                e.activeTime = def.activeTime.runNum(env);
                e.sleepTime = def.sleepTime.runNum(env);
            }
            case ONCE -> e.activeTime = def.activeTime.runNum(env);
            default -> e.activeTime = 0;
        }
        setEmitterVars(e);
        runEvents(def.emitterCreationEvents, e, null);
        e.vars = env.vars;
        return e;
    }

    private double[] originOfLocal(double[] local) {
        double[] r = rotateY(local, 180.0 - yaw);
        return new double[] {entityPos[0] + r[0], entityPos[1] + r[1], entityPos[2] + r[2]};
    }

    private void setEmitterVars(Emitter e) {
        ParticleDef d = e.def;
        e.env.setVar(d.vEmitterAge, e.age);
        e.env.setVar(d.vEmitterLifetime, e.activeTime);
        for (int i = 0; i < 4; i++) e.env.setVar(d.vEmitterRandom[i], e.rnd[i]);
    }

    private void setParticleVars(Emitter e, Particle p) {
        ParticleDef d = e.def;
        Env env = e.env;
        setEmitterVars(e);
        env.setVar(d.vParticleAge, p.age);
        env.setVar(d.vParticleLifetime, p.lifetime);
        for (int i = 0; i < 4; i++) env.setVar(d.vParticleRandom[i], p.rnd[i]);
    }

    private static void applyCurves(ParticleDef d, Env env) {
        for (ParticleDef.Curve c : d.curves) {
            double range = c.range.runNum(env);
            double t = range == 0 ? 0 : c.input.runNum(env) / range;
            env.setVar(c.varId, sampleCurve(c, t));
        }
    }

    private static double sampleCurve(ParticleDef.Curve c, double t) {
        double[] n = c.nodes;
        double f = Math.max(0, Math.min(1, t)) * (n.length - 1);
        int i = (int) Math.floor(f);
        if (i >= n.length - 1) return n[n.length - 1];
        double a = f - i;
        if (!c.catmull) return n[i] + (n[i + 1] - n[i]) * a;
        double p0 = n[Math.max(0, i - 1)], p1 = n[i], p2 = n[i + 1], p3 = n[Math.min(n.length - 1, i + 2)];
        double a2 = a * a, a3 = a2 * a;
        return 0.5 * (2 * p1 + (-p0 + p2) * a + (2 * p0 - 5 * p1 + 4 * p2 - p3) * a2 + (-p0 + 3 * p1 - 3 * p2 + p3) * a3);
    }

    private void updateEmitter(Emitter e, double dt) {
        ParticleDef d = e.def;
        Env env = e.env;
        env.vars = e.vars;
        e.age += dt;
        if (e.age >= e.maxAge) e.stopped = true;
        setEmitterVars(e);
        applyCurves(d, env);
        if (d.emitterPerUpdate.length > 0) Script.runAll(env, d.emitterPerUpdate);
        boolean active;
        boolean expired = false;
        switch (d.life) {
            case LOOPING -> {
                double cycle = e.activeTime + e.sleepTime;
                double t = cycle <= 0 ? 0 : e.age % cycle;
                active = t < e.activeTime;
            }
            case EXPRESSION -> {
                expired = Values.truthy(d.expiration.run(env));
                active = !expired && Values.truthy(d.activation.run(env));
            }
            default -> active = e.age < e.activeTime;
        }
        if (e.stopped) active = false;
        e.vars = env.vars;
        if (active && !e.wasActive && d.rate == ParticleDef.Rate.INSTANT) {
            int n = (int) Math.min(200, Math.max(0, d.numParticles.runNum(env)));
            for (int i = 0; i < n; i++) spawnParticle(e);
        }
        if (e.stopped) active = false;
        e.wasActive = active;
        e.active = active;
        if (active && d.rate == ParticleDef.Rate.STEADY) {
            int max = (int) Math.min(maxParticles, Math.max(0, d.maxParticles.runNum(env)));
            e.spawnAccumulator += Math.max(0, d.spawnRate.runNum(env)) * dt;
            while (e.spawnAccumulator >= 1) {
                e.spawnAccumulator -= 1;
                if (e.particles.size() < max) spawnParticle(e);
            }
        }
        for (Map.Entry<Double, List<String>> t : d.emitterTimeline.entrySet()) {
            if (t.getKey() <= e.age && e.fired.add(t.getKey())) runEvents(t.getValue(), e, null);
        }
        for (Particle p : e.particles) updateParticle(e, p, dt);
        e.particles.removeIf(p -> p.dead);
        e.vars = env.vars;
        if (e.stopped) {
            if (e.particles.isEmpty()) finish(e);
        } else if (d.life == ParticleDef.Life.EXPRESSION) {
            if (expired && e.particles.isEmpty()) finish(e);
        } else if (d.life == ParticleDef.Life.ONCE && !active && e.particles.isEmpty()) {
            finish(e);
        }
    }

    private void finish(Emitter e) {
        if (e.dead) return;
        e.env.vars = e.vars;
        runEvents(e.def.emitterExpirationEvents, e, null);
        e.vars = e.env.vars;
        e.dead = true;
    }

    private double[] evalVec(Script[] s, Env env) {
        return new double[] {s[0].runNum(env), s[1].runNum(env), s[2].runNum(env)};
    }

    private void spawnParticle(Emitter e) {
        if (particleCount() >= maxParticles) return;
        ParticleDef d = e.def;
        Env env = e.env;
        Particle p = new Particle();
        p.vars = Arrays.copyOf(e.vars, Math.max(e.vars.length, d.symbols.varCount()));
        for (int i = 0; i < 4; i++) p.rnd[i] = random.nextDouble();
        env.vars = p.vars;
        setParticleVars(e, p);
        p.lifetime = Math.max(0.001, d.maxLifetime.runNum(env));
        env.setVar(d.vParticleLifetime, p.lifetime);
        applyCurves(d, env);
        double[] off = {0, 0, 0};
        double[] dir = {0, 0, 0};
        sampleShape(d, env, off, dir);
        if (d.directionInwards) {
            dir[0] = -dir[0];
            dir[1] = -dir[1];
            dir[2] = -dir[2];
        }
        double[] vel = {0, 0, 0};
        if (d.speedVector != null) {
            vel = evalVec(d.speedVector, env);
        } else if (d.speed != null) {
            double sp = d.speed.runNum(env);
            vel = new double[] {dir[0] * sp, dir[1] * sp, dir[2] * sp};
        }
        if (d.localRotation) {
            off = rotateY(off, 180.0 - yaw);
            vel = rotateY(vel, 180.0 - yaw);
        }
        boolean local = d.localPosition || d.parametricPosition != null;
        if (local) {
            System.arraycopy(off, 0, p.pos, 0, 3);
        } else {
            double[] o = origin(e);
            for (int i = 0; i < 3; i++) p.pos[i] = o[i] + off[i];
        }
        System.arraycopy(vel, 0, p.vel, 0, 3);
        if (d.initialSpin != null) p.roll = d.initialSpin.runNum(env);
        if (d.initialSpinRate != null) p.rollRate = d.initialSpinRate.runNum(env);
        runEvents(d.particleCreationEvents, e, p);
        p.vars = env.vars;
        e.particles.add(p);
        env.vars = e.vars;
    }

    private void sampleShape(ParticleDef d, Env env, double[] off, double[] dir) {
        double[] base = d.shapeOffset == null ? new double[] {0, 0, 0} : evalVec(d.shapeOffset, env);
        double[] sample = {0, 0, 0};
        switch (d.shape) {
            case SPHERE -> {
                double r = d.radius == null ? 1 : d.radius.runNum(env);
                double[] u = randomUnit();
                double dist = d.surfaceOnly ? r : r * Math.cbrt(random.nextDouble());
                sample = new double[] {u[0] * dist, u[1] * dist, u[2] * dist};
            }
            case BOX -> {
                double[] h = d.halfDimensions == null ? new double[] {0, 0, 0} : evalVec(d.halfDimensions, env);
                sample = new double[] {(random.nextDouble() * 2 - 1) * h[0], (random.nextDouble() * 2 - 1) * h[1], (random.nextDouble() * 2 - 1) * h[2]};
                if (d.surfaceOnly) {
                    int axis = random.nextInt(3);
                    sample[axis] = random.nextBoolean() ? h[axis] : -h[axis];
                }
            }
            case DISC -> {
                double r = d.radius == null ? 1 : d.radius.runNum(env);
                double[] n = d.planeNormal == null ? new double[] {0, 1, 0} : evalVec(d.planeNormal, env);
                normalize(n);
                double[] a = Math.abs(n[1]) < 0.99 ? cross(new double[] {0, 1, 0}, n) : cross(new double[] {1, 0, 0}, n);
                normalize(a);
                double[] b = cross(n, a);
                double ang = random.nextDouble() * Math.PI * 2;
                double dist = d.surfaceOnly ? r : r * Math.sqrt(random.nextDouble());
                for (int i = 0; i < 3; i++) sample[i] = (a[i] * Math.cos(ang) + b[i] * Math.sin(ang)) * dist;
            }
            default -> {}
        }
        for (int i = 0; i < 3; i++) off[i] = base[i] + sample[i];
        if (d.direction != null) {
            double[] v = evalVec(d.direction, env);
            System.arraycopy(v, 0, dir, 0, 3);
        } else if (d.shape == ParticleDef.Shape.SPHERE || d.shape == ParticleDef.Shape.BOX || d.shape == ParticleDef.Shape.DISC) {
            System.arraycopy(sample, 0, dir, 0, 3);
            normalize(dir);
        }
    }

    private double[] randomUnit() {
        double z = random.nextDouble() * 2 - 1;
        double a = random.nextDouble() * Math.PI * 2;
        double r = Math.sqrt(1 - z * z);
        return new double[] {r * Math.cos(a), z, r * Math.sin(a)};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static void normalize(double[] v) {
        double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len < 1e-9) return;
        v[0] /= len;
        v[1] /= len;
        v[2] /= len;
    }

    private void updateParticle(Emitter e, Particle p, double dt) {
        ParticleDef d = e.def;
        Env env = e.env;
        env.vars = p.vars;
        p.age += dt;
        setParticleVars(e, p);
        applyCurves(d, env);
        if (d.particlePerUpdate.length > 0) Script.runAll(env, d.particlePerUpdate);
        if (d.parametricPosition != null) {
            double[] rel = evalVec(d.parametricPosition, env);
            System.arraycopy(rel, 0, p.pos, 0, 3);
        } else if (d.dynamic) {
            double[] acc = d.acceleration == null ? new double[3] : evalVec(d.acceleration, env);
            double drag = d.drag == null ? 0 : d.drag.runNum(env);
            for (int i = 0; i < 3; i++) {
                p.vel[i] += (acc[i] - drag * p.vel[i]) * dt;
                p.pos[i] += p.vel[i] * dt;
            }
            double rotAcc = d.rotationAcceleration == null ? 0 : d.rotationAcceleration.runNum(env);
            double rotDrag = d.rotationDrag == null ? 0 : d.rotationDrag.runNum(env);
            p.rollRate += (rotAcc - rotDrag * p.rollRate) * dt;
        }
        p.roll += p.rollRate * dt;
        for (Map.Entry<Double, List<String>> t : d.particleTimeline.entrySet()) {
            if (t.getKey() <= p.age && p.fired.add(t.getKey())) runEvents(t.getValue(), e, p);
        }
        boolean expired = p.age >= p.lifetime || (d.particleExpiration != null && Values.truthy(d.particleExpiration.run(env)));
        if (expired) {
            p.dead = true;
            runEvents(d.particleExpirationEvents, e, p);
        }
        p.vars = env.vars;
        env.vars = e.vars;
    }

    private void runEvents(List<String> names, Emitter e, Particle p) {
        if (names.isEmpty()) return;
        e.env.beginScope();
        for (String n : names) {
            ParticleDef.EventNode node = e.def.events.get(n);
            if (node != null) runNode(node, e, p, 0);
        }
    }

    private void runNode(ParticleDef.EventNode n, Emitter e, Particle p, int depth) {
        if (depth > 8) return;
        if (n.expression != null) n.expression.exec(e.env);
        if (n.sound != null) pendingSounds.add(n.sound);
        if (n.effect != null) spawnChild(n, e, p);
        if (n.sequence != null) for (ParticleDef.EventNode c : n.sequence) runNode(c, e, p, depth + 1);
        if (n.randomize != null && !n.randomize.isEmpty()) {
            double total = 0;
            for (double w : n.weights) total += w;
            double roll = random.nextDouble() * total;
            for (int i = 0; i < n.randomize.size(); i++) {
                roll -= n.weights.get(i);
                if (roll <= 0 || i == n.randomize.size() - 1) {
                    runNode(n.randomize.get(i), e, p, depth + 1);
                    break;
                }
            }
        }
    }

    private void spawnChild(ParticleDef.EventNode n, Emitter parent, Particle p) {
        if (parent.generation >= 4 || emitters.size() + incoming.size() >= maxEmitters) return;
        ParticleDef def = resolver.find(n.effect.toLowerCase(java.util.Locale.ROOT));
        if (def == null) return;
        boolean atParticle = n.effectType.startsWith("particle") && p != null;
        double[] fixed;
        boolean bound = n.effectType.equals("emitter_bound") && parent.bound;
        if (atParticle) {
            boolean local = parent.def.localPosition || parent.def.parametricPosition != null;
            if (local) {
                double[] o = origin(parent);
                double[] r = parent.def.localRotation ? rotateY(p.pos, 180.0 - yaw) : p.pos;
                fixed = new double[] {o[0] + r[0], o[1] + r[1], o[2] + r[2]};
            } else {
                fixed = p.pos.clone();
            }
        } else {
            fixed = origin(parent).clone();
        }
        Object[] savedVars = parent.env.vars;
        Emitter child = create(def, parent.localPos, bound, fixed, parent.generation + 1, n.preEffect, parent.env, false);
        parent.env.vars = savedVars;
        if (parent.stopped || parent.maxAge != Double.MAX_VALUE) child.maxAge = 3.0;
        incoming.add(child);
    }

    public void collect(double[] entityPosition, double yawDegrees, List<EmitterView> out) {
        for (Emitter e : emitters) {
            if (e.particles.isEmpty()) continue;
            ParticleDef d = e.def;
            if (d.size == null) continue;
            EmitterView view = new EmitterView();
            view.def = d;
            double[] o = origin(e);
            Env env = e.env;
            for (Particle p : e.particles) {
                env.vars = p.vars;
                setParticleVars(e, p);
                applyCurves(d, env);
                if (d.particlePerRender.length > 0) Script.runAll(env, d.particlePerRender);
                RenderParticle rp = new RenderParticle();
                double[] w;
                if (d.localPosition || d.parametricPosition != null) {
                    double[] r = d.localRotation ? rotateY(p.pos, 180.0 - yawDegrees) : p.pos;
                    w = new double[] {o[0] + r[0], o[1] + r[1], o[2] + r[2]};
                } else {
                    w = p.pos;
                }
                rp.x = w[0] - entityPosition[0];
                rp.y = w[1] - entityPosition[1];
                rp.z = w[2] - entityPosition[2];
                rp.vx = p.vel[0];
                rp.vy = p.vel[1];
                rp.vz = p.vel[2];
                rp.width = (float) (d.size[0].runNum(env) * sizeFactor);
                rp.height = (float) (d.size[1].runNum(env) * sizeFactor);
                rp.roll = (float) p.roll;
                uv(d, env, p, rp);
                rp.argb = tint(d, env);
                view.particles.add(rp);
                p.vars = env.vars;
            }
            env.vars = e.vars;
            if (!view.particles.isEmpty()) out.add(view);
        }
    }

    private static void uv(ParticleDef d, Env env, Particle p, RenderParticle rp) {
        double u = 0, v = 0, w = d.texWidth, h = d.texHeight;
        if (d.uvSize != null) {
            w = d.uvSize[0].runNum(env);
            h = d.uvSize[1].runNum(env);
        }
        if (d.flipbook && d.baseUv != null) {
            double max = d.maxFrame == null ? 1 : d.maxFrame.runNum(env);
            double frame = d.stretchToLifetime
                    ? p.age / p.lifetime * max
                    : (d.fps == null ? 0 : d.fps.runNum(env)) * p.age;
            frame = Math.floor(frame);
            if (max > 0) frame = d.loopFrames ? frame % max : Math.min(frame, max - 1);
            u = d.baseUv[0].runNum(env) + d.stepUv[0].runNum(env) * frame;
            v = d.baseUv[1].runNum(env) + d.stepUv[1].runNum(env) * frame;
        } else if (d.uv != null) {
            u = d.uv[0].runNum(env);
            v = d.uv[1].runNum(env);
        }
        double tw = d.texWidth <= 0 ? 1 : d.texWidth;
        double th = d.texHeight <= 0 ? 1 : d.texHeight;
        rp.u0 = (float) (u / tw);
        rp.v0 = (float) (v / th);
        rp.u1 = (float) ((u + w) / tw);
        rp.v1 = (float) ((v + h) / th);
    }

    private static int tint(ParticleDef d, Env env) {
        float r = 1, g = 1, b = 1, a = 1;
        if (d.gradient != null && d.gradientInterpolant != null) {
            double t = d.gradientInterpolant.runNum(env);
            Map.Entry<Double, float[]> lo = d.gradient.floorEntry(t);
            Map.Entry<Double, float[]> hi = d.gradient.ceilingEntry(t);
            float[] c;
            if (lo == null) c = hi.getValue();
            else if (hi == null) c = lo.getValue();
            else if (lo.getKey().equals(hi.getKey())) c = lo.getValue();
            else {
                double f = (t - lo.getKey()) / (hi.getKey() - lo.getKey());
                float[] x = lo.getValue(), y = hi.getValue();
                c = new float[] {(float) (x[0] + (y[0] - x[0]) * f), (float) (x[1] + (y[1] - x[1]) * f),
                    (float) (x[2] + (y[2] - x[2]) * f), (float) (x[3] + (y[3] - x[3]) * f)};
            }
            r = c[0];
            g = c[1];
            b = c[2];
            a = c[3];
        } else if (d.color != null) {
            r = (float) d.color[0].runNum(env);
            g = (float) d.color[1].runNum(env);
            b = (float) d.color[2].runNum(env);
            a = (float) d.color[3].runNum(env);
        }
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255f)));
    }
}
