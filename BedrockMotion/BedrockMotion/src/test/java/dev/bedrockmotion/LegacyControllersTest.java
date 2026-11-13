package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.EntityAnimator;
import dev.bedrockmotion.bedrock.EntityStack;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LegacyControllersTest {

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
        Path dir = Files.createTempDirectory("bm-legacy");
        write(dir, "manifest.json", "{\"format_version\":2,\"header\":{\"name\":\"t\",\"uuid\":\"00000000-0000-0000-0000-000000000002\",\"version\":[1,0,0]},\"modules\":[]}");
        write(dir, "entity/old.entity.json", """
                {"format_version":"1.8.0","minecraft:client_entity":{"description":{"identifier":"test:old",
                "animations":{"move":"animation.test.move"},
                "animation_controllers":[{"move":"controller.animation.test.move"}]}}}""");
        write(dir, "entity/mixed.entity.json", """
                {"format_version":"1.10.0","minecraft:client_entity":{"description":{"identifier":"test:mixed",
                "animations":{"move":"animation.test.move","other":"animation.test.other"},
                "animation_controllers":[{"other":"controller.animation.test.other"}],
                "scripts":{"animate":["move"]}}}}""");
        write(dir, "animations/test.animation.json", """
                {"format_version":"1.8.0","animations":{
                "animation.test.move":{"loop":true,"animation_length":1,"bones":{"moving":{"rotation":[30,0,0]}}},
                "animation.test.other":{"loop":true,"animation_length":1,"bones":{"other":{"rotation":[60,0,0]}}}}}""");
        write(dir, "animation_controllers/test.animation_controllers.json", """
                {"format_version":"1.10.0","animation_controllers":{
                "controller.animation.test.move":{"initial_state":"default","states":{"default":{"animations":["move"]}}},
                "controller.animation.test.other":{"initial_state":"default","states":{"default":{"animations":["other"]}}}}}""");
        try (PackFiles files = PackFiles.open(dir)) {
            BedrockPack pack = BedrockPack.load("test", files, null);
            BedrockPack.stack(List.of(pack));
            var defs = EntityStack.build(List.of(pack)).entities();
            ClientEntityDef old = defs.get("test:old");
            EntityAnimator legacy = new EntityAnimator(old);
            for (int f = 0; f < 4; f++) legacy.update(0.05, f * 0.05, 0.5);
            check("legacy controller runs without animate script", rotationX(legacy, "moving") > 25);
            ClientEntityDef mixed = defs.get("test:mixed");
            EntityAnimator both = new EntityAnimator(mixed);
            for (int f = 0; f < 4; f++) both.update(0.05, f * 0.05, 0.5);
            check("animate script still runs", rotationX(both, "moving") > 25);
            check("legacy list is ignored next to an animate script", rotationX(both, "other") == 0);
        }
        System.out.println(failures == 0 ? "LegacyControllersTest: all tests passed" : "LegacyControllersTest: " + failures + " errors");
        if (failures > 0) System.exit(1);
    }
}
