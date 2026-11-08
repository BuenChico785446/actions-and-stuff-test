package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.*;
import dev.bedrockmotion.molang.EntitySnapshot;
import dev.bedrockmotion.molang.Queries;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.*;

public final class StackSmoke {
    public static void main(String[] args) throws Exception {
        Set<String> missingQueries = new TreeSet<>();
        Queries.setMissingHandler(missingQueries::add);
        try (PackFiles baseFiles = PackFiles.open(Path.of(args[0])); PackFiles addonFiles = PackFiles.open(Path.of(args[1]))) {
            BedrockPack base = BedrockPack.load("base_vanilla", baseFiles, null);
            BedrockPack addon = BedrockPack.load("addon", addonFiles, null);
            base.baseOnly = true;
            BedrockPack.stack(List.of(base, addon));
            EntityStack.Result stacked = EntityStack.build(List.of(base, addon));
            List<ClientEntityDef> defs = new ArrayList<>(stacked.entities().values());
            defs.addAll(stacked.attachables().values());
            int withLayer = 0, errors = 0, noLayer = 0;
            long nanos = 0, frames = 0;
            List<String> noLayerIds = new ArrayList<>();
            for (ClientEntityDef def : defs) {
                EntityAnimator an = new EntityAnimator(def);
                boolean any = false;
                try {
                    for (int f = 0; f < 90; f++) {
                        EntitySnapshot s = an.env.entity;
                        s.groundSpeed = f < 45 ? 3 : 0;
                        s.modifiedMoveSpeed = s.groundSpeed;
                        s.modifiedDistanceMoved += s.groundSpeed * 0.05;
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
                        any |= !plan.layers.isEmpty();
                        an.pendingSounds.clear();
                        an.pendingParticles.clear();
                    }
                } catch (RuntimeException e) {
                    errors++;
                    System.out.println("ERROR " + def.identifier + ": " + e);
                }
                if (any) withLayer++; else { noLayer++; noLayerIds.add(def.identifier); }
            }
            System.out.printf(Locale.ROOT, "definitions: %d, with render layer: %d, without: %d, errors: %d, %.3f ms/frame%n", defs.size(), withLayer, noLayer, errors, nanos / 1e6 / Math.max(1, frames));
            System.out.println("no render layer: " + noLayerIds);
            System.out.println("missing queries: " + missingQueries);
        }
    }
}
