package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.EntityAnimator;
import dev.bedrockmotion.molang.EntitySnapshot;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class StackTest {
    public static void main(String[] args) throws Exception {
        try (PackFiles smoothFiles = PackFiles.open(Path.of(args[0])); PackFiles baseFiles = PackFiles.open(Path.of(args[1]))) {
            BedrockPack smooth = BedrockPack.load("smooth", smoothFiles, null);
            BedrockPack base = BedrockPack.load("base_vanilla", baseFiles, null);
            String id = "animation.humanoid.base_pose";
            System.out.println("before stacking: smooth.animation(base_pose) = " + smooth.animation(id));
            BedrockPack.stack(List.of(base, smooth));
            System.out.println("after  stacking: smooth.animation(base_pose) = " + (smooth.animation(id) != null ? "found" : "MISSING")
                    + ", base sees smooth = " + (base.animation("animation.player.shield.look") != null));
            ClientEntityDef def = smooth.entities.get("minecraft:player");
            EntityAnimator an = new EntityAnimator(def);
            EntitySnapshot s = an.env.entity;
            for (int f = 0; f < 6; f++) an.update(0.05, f * 0.05, 0.5);
            an.pose.forEachTouched((name, x) -> {
                if (!x.isIdentity()) System.out.printf(Locale.ROOT, "  bone %-10s rot[%.1f %.1f %.1f]%n", name, Math.toDegrees(x.rx), Math.toDegrees(x.ry), Math.toDegrees(x.rz));
            });
            boolean ok = smooth.animation(id) != null && base.animation("animation.player.shield.look") == null;
            System.out.println(ok ? "stack OK" : "stack FAILED");
            if (!ok) System.exit(1);
        }
    }
}
