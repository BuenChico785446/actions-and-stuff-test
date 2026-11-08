package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.EntityAnimator;
import dev.bedrockmotion.bedrock.GeometryDef;
import dev.bedrockmotion.bedrock.ParticleDef;
import dev.bedrockmotion.bedrock.ParticleSystem;
import dev.bedrockmotion.bedrock.RenderPlan;
import dev.bedrockmotion.bedrock.RenderPlanner;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class BindingParticleTest {

    public static void main(String[] args) throws Exception {
        int failures = 0;
        try (PackFiles files = PackFiles.open(Path.of(args[0]))) {
            BedrockPack pack = BedrockPack.load("x", files, "SP2");
            GeometryDef owner = null;
            for (GeometryDef g : pack.geometries.values()) {
                if (g.indexOf("rightitem") >= 0 && g.indexOf("head") >= 0 && g.indexOf("body") >= 0) {
                    owner = g;
                    break;
                }
            }
            if (owner == null) {
                System.out.println("no owner geometry with rightitem found");
                System.exit(1);
            }
            final GeometryDef ownerGeometry = owner;
            Function<String, String> resolver = slot -> switch (String.valueOf(slot)) {
                case "main_hand" -> "rightitem";
                case "off_hand" -> ownerGeometry.indexOf("leftitem") >= 0 ? "leftitem" : "";
                case "head" -> "head";
                case "torso" -> "body";
                default -> "";
            };
            int attachables = 0;
            int withBinding = 0;
            int resolved = 0;
            int errors = 0;
            for (ClientEntityDef def : pack.attachables.values()) {
                attachables++;
                try {
                    EntityAnimator an = new EntityAnimator(def);
                    an.env.context.put("item_slot", "main_hand");
                    an.env.context.put("is_first_person", 0.0);
                    an.env.context.put("slot_bone", resolver);
                    for (int f = 0; f < 3; f++) an.update(0.05, f * 0.05, 0.5);
                    RenderPlan plan = RenderPlanner.plan(an);
                    for (RenderPlan.Layer l : plan.layers) {
                        if (l.bindings == null) continue;
                        withBinding++;
                        for (String b : l.bindings) {
                            if (b != null && ownerGeometry.indexOf(b) >= 0) {
                                resolved++;
                                break;
                            }
                        }
                    }
                } catch (RuntimeException ex) {
                    errors++;
                    if (errors < 4) System.out.println("  Error in " + def.identifier + ": " + ex);
                }
            }
            System.out.printf("attachables: %d, layers with bindings: %d, resolved against owner bones: %d, errors: %d%n",
                    attachables, withBinding, resolved, errors);
            failures += errors;
            if (withBinding == 0 || resolved == 0) failures++;

            int checked = 0;
            int ok = 0;
            int respawned = 0;
            for (ParticleDef def : pack.particles.values()) {
                if (def.life != ParticleDef.Life.LOOPING && def.life != ParticleDef.Life.EXPRESSION && def.rate != ParticleDef.Rate.STEADY) continue;
                ParticleSystem system = new ParticleSystem(pack.particles::get, new dev.bedrockmotion.molang.Env(pack.symbols));
                double[] pos = {0, 64, 0};
                system.spawn(def.id, new double[] {0, 1, 0}, null, pos, 0);
                int peak = 0;
                for (int i = 0; i < 40; i++) {
                    system.update(0.05, pos, 0);
                    peak = Math.max(peak, system.particleCount());
                }
                if (peak == 0) continue;
                checked++;
                system.detach();
                double elapsed = 0;
                int after = system.particleCount();
                int maxAfter = after;
                while (!system.isEmpty() && elapsed < 600) {
                    system.updateDetached(0.05);
                    elapsed += 0.05;
                    maxAfter = Math.max(maxAfter, system.particleCount());
                }
                if (system.isEmpty()) ok++;
                if (maxAfter > after) respawned++;
            }
            System.out.printf("detached emitters: %d with particles, %d expired completely, %d spawned one-shot follow-ups%n", checked, ok, respawned);
            if (checked == 0 || ok < checked) failures++;
        }
        if (failures > 0) System.exit(1);
    }
}
