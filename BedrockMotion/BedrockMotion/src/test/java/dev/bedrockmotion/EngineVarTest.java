package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.EntityAnimator;
import dev.bedrockmotion.bedrock.Pose;
import dev.bedrockmotion.molang.EntitySnapshot;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class EngineVarTest {

    static int failures;

    static double[] sample(BedrockPack pack, String id, String bone, Consumer<EntitySnapshot> setup, int frames) {
        ClientEntityDef def = pack.entities.get(id);
        EntityAnimator an = new EntityAnimator(def);
        double[] out = new double[3];
        for (int f = 0; f < frames; f++) {
            EntitySnapshot s = an.env.entity;
            s.position[0] = f * 0.1;
            setup.accept(s);
            an.update(0.05, f * 0.05, 0.5);
        }
        Pose.Xf x = an.pose.find(bone);
        if (x != null) {
            out[0] = Math.toDegrees(x.rx);
            out[1] = Math.toDegrees(x.ry);
            out[2] = Math.toDegrees(x.rz);
        }
        return out;
    }

    static String fmt(double[] v) {
        return String.format(Locale.ROOT, "[%.1f, %.1f, %.1f]", v[0], v[1], v[2]);
    }

    static void expectDifferent(String what, double[] a, double[] b) {
        boolean diff = false;
        for (int i = 0; i < 3; i++) diff |= Math.abs(a[i] - b[i]) > 0.01;
        System.out.println((diff ? "  OK   " : "  FAIL ") + what + ": " + fmt(a) + " vs " + fmt(b));
        if (!diff) failures++;
    }

    public static void main(String[] args) throws Exception {
        try (PackFiles files = PackFiles.open(Path.of(args[0]))) {
            BedrockPack pack = BedrockPack.load("smooth", files, null);
            System.out.println("Pack: " + pack.summary());

            System.out.println("1) swimming zombie: variable.swim_amount switches the swim animation on, variable.attack_time drives the stroke");
            for (String id : List.of("minecraft:zombie", "minecraft:drowned", "minecraft:husk")) {
                if (!pack.entities.containsKey(id)) { System.out.println("  (no " + id + ")"); continue; }
                double[] dry = sample(pack, id, "rightarm", s -> s.engineVars.put("swim_amount", 0.0), 6);
                double[] swimIdle = sample(pack, id, "rightarm", s -> { s.engineVars.put("swim_amount", 1.0); s.engineVars.put("attack_time", 0.0); }, 6);
                double[] swimStroke = sample(pack, id, "rightarm", s -> { s.engineVars.put("swim_amount", 1.0); s.engineVars.put("attack_time", 0.5); }, 6);
                expectDifferent(id + " rightarm dry vs swimming", dry, swimIdle);
                expectDifferent(id + " rightarm swim idle vs swim stroke (attack_time)", swimIdle, swimStroke);
            }

            System.out.println("2) walk cycle divides by variable.gliding_speed_value");
            for (String id : List.of("minecraft:zombie", "minecraft:player")) {
                for (boolean seeded : new boolean[] {true, false}) {
                    ClientEntityDef def = pack.entities.get(id);
                    EntityAnimator an = new EntityAnimator(def);
                    for (int f = 0; f < 5; f++) {
                        an.env.entity.modifiedMoveSpeed = 4;
                        an.env.entity.modifiedDistanceMoved += 0.3;
                        if (!seeded) an.env.entity.engineVars.remove("gliding_speed_value");
                        an.update(0.05, f * 0.05, 0.5);
                    }
                    Object v = an.env.getVar(an.env.symbols.findVar("tcos0"));
                    System.out.println("  " + id + " seeded=" + seeded + " tcos0=" + v);
                }
            }

            System.out.println("3) item use: v.item_use_duration / v.item_use_normalized come from main_hand_item_*_duration");
            for (boolean using : new boolean[] {false, true}) {
                ClientEntityDef def = pack.entities.get("minecraft:player");
                EntityAnimator an = new EntityAnimator(def);
                for (int f = 0; f < 5; f++) {
                    EntitySnapshot s = an.env.entity;
                    s.usingItem = using;
                    s.mainHandItemUseDuration = using ? 72000 - 10 - f : 0;
                    s.mainHandItemMaxDuration = using ? 72000 : 0;
                    an.update(0.05, f * 0.05, 0.5);
                }
                Object d = an.env.getVar(an.env.symbols.findVar("item_use_duration"));
                Object n = an.env.getVar(an.env.symbols.findVar("item_use_normalized"));
                System.out.println("  using=" + using + " item_use_duration=" + d + " item_use_normalized=" + n);
                if (using && !(d instanceof Number dn && dn.doubleValue() > 5)) { failures++; System.out.println("  FAIL item_use_duration should count elapsed ticks"); }
            }
        }
        if (failures > 0) { System.out.println(failures + " checks failed"); System.exit(1); }
        System.out.println("all checks passed");
    }
}
