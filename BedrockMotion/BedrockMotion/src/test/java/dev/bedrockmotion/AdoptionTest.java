package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.EntityAnimator;
import dev.bedrockmotion.bedrock.EntityStack;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class AdoptionTest {

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

    private static String manifest(int n) {
        return "{\"format_version\":2,\"header\":{\"name\":\"t\",\"uuid\":\"00000000-0000-0000-0000-00000000000" + n + "\",\"version\":[1,0,0]},\"modules\":[]}";
    }

    private static String entity(String id, String animation, String geometry) {
        return "{\"format_version\":\"1.10.0\",\"minecraft:client_entity\":{\"description\":{\"identifier\":\"" + id + "\","
                + "\"animations\":{\"move\":\"" + animation + "\"},\"geometry\":{\"default\":\"" + geometry + "\"},"
                + "\"scripts\":{\"animate\":[\"move\"]}}}}";
    }

    private static String animation(String id, int degrees) {
        return "\"" + id + "\":{\"loop\":true,\"animation_length\":1,\"bones\":{\"moving\":{\"rotation\":[" + degrees + ",0,0]}}}";
    }

    private static double rotationX(EntityAnimator animator) {
        double[] out = {0};
        animator.pose.forEachTouched((name, xf) -> {
            if (name.equalsIgnoreCase("moving")) out[0] = Math.abs(Math.toDegrees(xf.rx));
        });
        return out[0];
    }

    public static void main(String[] args) throws Exception {
        Path base = Files.createTempDirectory("bm-adopt-base");
        Path addon = Files.createTempDirectory("bm-adopt-addon");
        write(base, "manifest.json", manifest(3));
        write(base, "entity/adopted.entity.json", entity("test:adopted", "animation.test.changed", "geometry.base"));
        write(base, "entity/untouched.entity.json", entity("test:untouched", "animation.test.same", "geometry.base"));
        write(base, "entity/geo.entity.json", entity("test:geoonly", "animation.test.same", "geometry.shared"));
        write(base, "animations/test.animation.json", "{\"format_version\":\"1.8.0\",\"animations\":{" + animation("animation.test.changed", 10) + "," + animation("animation.test.same", 10) + "}}");
        write(addon, "manifest.json", manifest(4));
        write(addon, "animations/test.animation.json", "{\"format_version\":\"1.8.0\",\"animations\":{" + animation("animation.test.changed", 45) + "}}");
        write(addon, "models/entity/shared.geo.json", "{\"format_version\":\"1.8.0\",\"geometry.shared\":{\"bones\":[{\"name\":\"moving\",\"pivot\":[0,0,0],\"cubes\":[{\"origin\":[-1,0,-1],\"size\":[2,2,2],\"uv\":[0,0]}]}]}}");
        try (PackFiles baseFiles = PackFiles.open(base); PackFiles addonFiles = PackFiles.open(addon)) {
            BedrockPack basePack = BedrockPack.load("base", baseFiles, null);
            basePack.baseOnly = true;
            BedrockPack addonPack = BedrockPack.load("addon", addonFiles, null);
            BedrockPack.stack(List.of(basePack, addonPack));
            Map<String, ClientEntityDef> defs = EntityStack.build(List.of(basePack, addonPack)).entities();
            check("a base def whose animation is overridden becomes active", defs.containsKey("test:adopted"));
            check("a base def nobody overrides stays inactive", !defs.containsKey("test:untouched"));
            check("a base def with only a geometry override stays inactive", !defs.containsKey("test:geoonly"));
            if (defs.containsKey("test:adopted")) {
                EntityAnimator animator = new EntityAnimator(defs.get("test:adopted"));
                for (int f = 0; f < 3; f++) animator.update(0.05, f * 0.05, 0.5);
                check("the overriding animation is the one that plays", rotationX(animator) > 40);
            }
        }
        System.out.println(failures == 0 ? "AdoptionTest: all tests passed" : "AdoptionTest: " + failures + " errors");
        if (failures > 0) System.exit(1);
    }
}
