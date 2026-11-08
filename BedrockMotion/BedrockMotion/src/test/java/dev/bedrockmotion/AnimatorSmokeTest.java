package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.EntityAnimator;
import dev.bedrockmotion.bedrock.LocatorSolver;
import dev.bedrockmotion.bedrock.ParticleSystem;
import dev.bedrockmotion.bedrock.Pose;
import dev.bedrockmotion.bedrock.RenderPlan;
import dev.bedrockmotion.bedrock.RenderPlanner;
import dev.bedrockmotion.molang.EntitySnapshot;
import dev.bedrockmotion.molang.Queries;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public final class AnimatorSmokeTest {

    public static void main(String[] args) throws Exception {
        Queries.setMissingHandler(n -> {});
        int failures = 0;
        for (String arg : args) {
            try (PackFiles files = PackFiles.open(Path.of(arg))) {
                List<String> subs = BedrockPack.subpacks(files);
                BedrockPack pack = BedrockPack.load(arg, files, subs.isEmpty() ? null : subs.get(subs.size() - 1));
                List<ClientEntityDef> defs = new ArrayList<>(pack.entities.values());
                defs.addAll(pack.attachables.values());
                int withLayer = 0;
                int bad = 0;
                int layers = 0;
                int sounds = 0;
                int requests = 0;
                int collected = 0;
                int located = 0;
                long nanos = 0;
                long frames = 0;
                for (ClientEntityDef def : defs) {
                    EntityAnimator an = new EntityAnimator(def);
                    ParticleSystem particles = new ParticleSystem(pack.particles::get, an.env);
                    boolean any = false;
                    try {
                        for (int f = 0; f < 90; f++) {
                            EntitySnapshot s = an.env.entity;
                            s.groundSpeed = f < 45 ? 0.2 : 0;
                            s.moving = f < 45;
                            s.position[0] = f * 0.1;
                            s.bodyYRotation = f * 3;
                            s.headYRotation = f * 3 + 10;
                            s.targetYRotation = Math.sin(f * 0.1) * 20;
                            s.sneaking = f > 60;
                            long t0 = System.nanoTime();
                            an.update(0.05, f * 0.05, 0.5);
                            RenderPlan plan = RenderPlanner.plan(an);
                            nanos += System.nanoTime() - t0;
                            frames++;
                            sounds += an.pendingSounds.size();
                            an.pendingSounds.clear();
                            double[] pos = {f * 0.1, 64, 0};
                            for (EntityAnimator.ParticleRequest r : an.pendingParticles) {
                                requests++;
                                double[] local = {0, 0, 0};
                                if (r.locator() != null && !plan.layers.isEmpty()) {
                                    double[] solved = LocatorSolver.position(plan.layers.get(0).geometry, an.pose, r.locator());
                                    if (solved != null) {
                                        local = solved;
                                        located++;
                                    }
                                }
                                particles.spawn(r.particleId(), local, r.preEffect(), pos, f * 3);
                            }
                            an.pendingParticles.clear();
                            particles.update(0.05, pos, f * 3);
                            particles.pendingSounds.clear();
                            if (f == 89) {
                                List<ParticleSystem.EmitterView> views = new ArrayList<>();
                                particles.collect(pos, f * 3, views);
                                for (ParticleSystem.EmitterView v : views) collected += v.particles.size();
                            }
                            if (f == 89) {
                                layers += plan.layers.size();
                                any = !plan.layers.isEmpty();
                            }
                        }
                        if (any) withLayer++;
                        for (String key : new TreeSet<>(java.util.List.of(def.identifier))) {
                            if (key.isEmpty()) bad++;
                        }
                        Pose p = an.pose;
                        if (p.size() > 0) {
                            for (String b : List.of("head", "body")) {
                                Pose.Xf x = p.find(b);
                                if (x != null && (Double.isNaN(x.rx) || Double.isNaN(x.py) || Double.isInfinite(x.sx))) bad++;
                            }
                        }
                    } catch (RuntimeException ex) {
                        bad++;
                        if (bad < 6) System.out.println("  Error in " + def.identifier + ": " + ex);
                    }
                }
                System.out.printf(Locale.ROOT, "%s: %d definitions, %d with render layer, %d layers, %d errors, %.3f ms/frame%n",
                        arg, defs.size(), withLayer, layers, bad, nanos / 1e6 / Math.max(1, frames));
                System.out.println("  Sound events queued: " + sounds + ", particle requests: " + requests
                        + " (locators solved: " + located + "), live particles at the end: " + collected);
                System.out.println("  Missing queries: " + new TreeSet<>(Queries.missing()));
                failures += bad;
            }
        }
        if (failures > 0) System.exit(1);
    }
}
