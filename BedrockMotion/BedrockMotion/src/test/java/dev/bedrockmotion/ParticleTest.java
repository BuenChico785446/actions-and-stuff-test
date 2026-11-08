package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.ParticleDef;
import dev.bedrockmotion.bedrock.ParticleSystem;
import dev.bedrockmotion.molang.Env;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ParticleTest {

    public static void main(String[] args) throws Exception {
        int failures = 0;
        for (String arg : args) {
            try (PackFiles files = PackFiles.open(Path.of(arg))) {
                List<String> subs = BedrockPack.subpacks(files);
                BedrockPack pack = BedrockPack.load(arg, files, subs.isEmpty() ? null : subs.get(subs.size() - 1));
                int ok = 0;
                int bad = 0;
                int withParticles = 0;
                int spawnedTotal = 0;
                int noSize = 0;
                long nanos = 0;
                long steps = 0;
                for (ParticleDef def : pack.particles.values()) {
                    Env owner = new Env(pack.symbols);
                    ParticleSystem system = new ParticleSystem(pack.particles::get, owner);
                    double[] pos = {10, 64, 10};
                    try {
                        system.spawn(def.id, new double[] {0, 1, 0}, null, pos, 45);
                        int peak = 0;
                        for (int i = 0; i < 80; i++) {
                            long t0 = System.nanoTime();
                            system.update(0.05, pos, 45);
                            List<ParticleSystem.EmitterView> views = new ArrayList<>();
                            system.collect(pos, 45, views);
                            nanos += System.nanoTime() - t0;
                            steps++;
                            int n = system.particleCount();
                            peak = Math.max(peak, n);
                            for (ParticleSystem.EmitterView v : views) {
                                for (ParticleSystem.RenderParticle p : v.particles) {
                                    if (Double.isNaN(p.x) || Double.isNaN(p.y) || Double.isNaN(p.z) || Float.isNaN(p.width)
                                            || Float.isNaN(p.u0) || Float.isInfinite(p.width)) {
                                        throw new IllegalStateException("NaN in render particle");
                                    }
                                }
                            }
                        }
                        if (def.size == null) noSize++;
                        if (peak > 0) withParticles++;
                        spawnedTotal += peak;
                        ok++;
                    } catch (RuntimeException ex) {
                        bad++;
                        if (bad < 5) System.out.println("  Error in " + def.id + ": " + ex);
                    }
                }
                System.out.printf(Locale.ROOT, "%s: %d particle defs, %d ok, %d errors, %d spawned particles at least once, %d without size, %.4f ms/step%n",
                        arg, pack.particles.size(), ok, bad, withParticles, noSize, nanos / 1e6 / Math.max(1, steps));
                failures += bad;
            }
        }
        if (failures > 0) System.exit(1);
    }
}
