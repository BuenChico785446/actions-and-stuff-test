package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.EntityAnimator;
import dev.bedrockmotion.bedrock.EntityStack;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ControllerFinishTest {

    private static int failures;

    private static void check(String what, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("ERROR " + what);
        }
    }

    private static void write(Path root, String file, String content) throws Exception {
        Path target = root.resolve(file);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }

    private static double rotationX(EntityAnimator animator, String bone) {
        double[] out = {0};
        animator.pose.forEachTouched((name, xf) -> {
            if (name.equalsIgnoreCase(bone)) out[0] = Math.abs(Math.toDegrees(xf.rx));
        });
        return out[0];
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("bm-finish");
        write(dir, "manifest.json", "{\"format_version\":2,\"header\":{\"name\":\"t\",\"uuid\":\"00000000-0000-0000-0000-000000000001\",\"version\":[1,0,0]},\"modules\":[]}");
        write(dir, "entity/mob.entity.json", """
                {"format_version":"1.10.0","minecraft:client_entity":{"description":{"identifier":"test:mob",
                "animations":{"ctl":"controller.animation.test.ctl","spin":"animation.test.spin","mark":"animation.test.mark"},
                "scripts":{"animate":["ctl"]}}}}""");
        write(dir, "animations/test.animation.json", """
                {"format_version":"1.8.0","animations":{
                "animation.test.spin":{"loop":true,"animation_length":1,"bones":{"spin":{"rotation":[45,0,0]}}},
                "animation.test.mark":{"loop":"hold_on_last_frame","animation_length":1,"bones":{"mark":{"rotation":[90,0,0]}}}}}""");
        write(dir, "animation_controllers/test.animation_controllers.json", """
                {"format_version":"1.10.0","animation_controllers":{"controller.animation.test.ctl":{"initial_state":"idle","states":{
                "idle":{"transitions":[{"looping":"q.all_animations_finished"}]},
                "looping":{"animations":["spin"],"transitions":[{"done":"v.go == 1 && q.all_animations_finished"}]},
                "done":{"animations":["mark"]}}}}}""");
        try (PackFiles files = PackFiles.open(dir)) {
            BedrockPack pack = BedrockPack.load("test", files, null);
            BedrockPack.stack(List.of(pack));
            ClientEntityDef def = EntityStack.build(List.of(pack)).entities().get("test:mob");
            EntityAnimator animator = new EntityAnimator(def);
            animator.env.setVar(animator.env.symbols.varId("go"), 1.0);
            double dt = 0.05;
            for (int f = 0; f < 3; f++) animator.update(dt, f * dt, 0.5);
            check("a state without animations counts as finished", rotationX(animator, "spin") > 40);
            check("loop is still running", rotationX(animator, "mark") == 0);
            for (int f = 3; f < 15; f++) {
                animator.update(dt, f * dt, 0.5);
                check("not finished before one pass at frame " + f, rotationX(animator, "mark") == 0);
            }
            for (int f = 15; f < 30; f++) animator.update(dt, f * dt, 0.5);
            check("a looping animation counts as finished after one pass", rotationX(animator, "mark") > 80);
        }
        System.out.println(failures == 0 ? "ControllerFinishTest: all tests passed" : "ControllerFinishTest: " + failures + " errors");
        if (failures > 0) System.exit(1);
    }
}
