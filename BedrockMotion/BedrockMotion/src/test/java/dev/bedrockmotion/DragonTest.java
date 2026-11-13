package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.EntityAnimator;
import dev.bedrockmotion.bedrock.EntityStack;
import dev.bedrockmotion.molang.Struct;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.List;

public final class DragonTest {

    private static int failures;

    private static void check(String what, double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-6) {
            failures++;
            System.out.println("ERROR " + what + ": " + actual + ", expected " + expected);
        }
    }

    private static double var(EntityAnimator animator, String name) {
        Object value = animator.env.getVar(animator.env.symbols.varId(name));
        return value instanceof Number n ? n.doubleValue() : Double.NaN;
    }

    public static void main(String[] args) throws Exception {
        try (PackFiles files = PackFiles.open(Path.of(args[0]))) {
            BedrockPack base = BedrockPack.load("base_vanilla", files, null);
            BedrockPack.stack(List.of(base));
            ClientEntityDef def = EntityStack.build(List.of(base)).entities().get("minecraft:ender_dragon");
            EntityAnimator animator = new EntityAnimator(def);
            animator.env.entity.extra.put("wing_flap_position", 0.0);
            for (int i = 0; i < 24; i++) {
                Struct frame = new Struct();
                frame.fields.put("rot_y", 100.0 - i * 3.0);
                frame.fields.put("pos_y", 64.0 + i * 0.5);
                animator.env.entity.engineStructs.put("historical_frame_" + i, frame);
            }
            animator.update(0.05, 1.0, 0.5);
            check("neck 1 yaw follows the history", var(animator, "neck_1_rotation_y"), 4.5);
            check("neck 1 pitch follows the history", var(animator, "neck_1_rotation_x"), Math.cos(0.0) * 8.6 + (-0.5) * 1.5 * 5.0);
            check("neck 2 position uses neck 1", var(animator, "neck_2_position_y"),
                    -20.0 - Math.sin(Math.toRadians(var(animator, "neck_1_rotation_x"))) * 10.0);
            for (String name : List.of("tail_12_position_x", "tail_12_rotation_y", "head_rotation_z", "clamped_roll", "clamped_pitch")) {
                double value = var(animator, name);
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    failures++;
                    System.out.println("ERROR " + name + " is " + value);
                }
            }
        }
        System.out.println(failures == 0 ? "DragonTest: all tests passed" : "DragonTest: " + failures + " errors");
        if (failures > 0) System.exit(1);
    }
}
