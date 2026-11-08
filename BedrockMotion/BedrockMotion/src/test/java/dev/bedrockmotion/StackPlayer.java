package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.*;
import dev.bedrockmotion.molang.EntitySnapshot;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.*;

public final class StackPlayer {
    public static void main(String[] args) throws Exception {
        try (PackFiles baseFiles = PackFiles.open(Path.of(args[0])); PackFiles addonFiles = PackFiles.open(Path.of(args[1]))) {
            BedrockPack base = BedrockPack.load("base_vanilla", baseFiles, null);
            BedrockPack addon = BedrockPack.load("addon", addonFiles, null);
            base.baseOnly = true;
            BedrockPack.stack(List.of(base, addon));
            EntityStack.Result r = EntityStack.build(List.of(base, addon));

            ClientEntityDef player = r.entities().get("minecraft:player");
            EntityAnimator an = new EntityAnimator(player);
            for (int f = 0; f < 40; f++) {
                EntitySnapshot s = an.env.entity;
                s.groundSpeed = 4.3; s.modifiedMoveSpeed = 1.0; s.modifiedDistanceMoved += 0.7; s.moving = true;
                an.update(0.05, f * 0.05, 0.5);
            }
            RenderPlan plan = RenderPlanner.plan(an);
            System.out.println("player layers: " + plan.layers.size());
            for (RenderPlan.Layer l : plan.layers) System.out.println("   geometry bones=" + l.geometry.bones.length + " texture=" + l.texturePath);
            System.out.println("player pose (walking, third person):");
            an.pose.forEachTouched((name, x) -> {
                if (!x.isIdentity()) System.out.printf(Locale.ROOT, "   %-12s rot[%6.1f %6.1f %6.1f] pos[%5.2f %5.2f %5.2f]%n", name, Math.toDegrees(x.rx), Math.toDegrees(x.ry), Math.toDegrees(x.rz), x.px, x.py, x.pz);
            });

            ClientEntityDef sword = r.attachables().get("minecraft:iron_sword");
            if (sword != null) {
                EntityAnimator sa = new EntityAnimator(sword);
                for (int f = 0; f < 3; f++) sa.update(0.05, f * 0.05, 0.5);
                RenderPlan sp = RenderPlanner.plan(sa);
                System.out.println("iron_sword attachable layers: " + sp.layers.size());
                for (RenderPlan.Layer l : sp.layers) {
                    StringBuilder bones = new StringBuilder();
                    for (GeometryDef.Bone b : l.geometry.bones) bones.append(b.name).append(' ');
                    System.out.println("   texture=" + l.texturePath + " bones=[" + bones.toString().trim() + "]");
                }
                sa.pose.forEachTouched((name, x) -> {
                    if (!x.isIdentity()) System.out.printf(Locale.ROOT, "   anim %-12s rot[%6.1f %6.1f %6.1f] pos[%5.2f %5.2f %5.2f]%n", name, Math.toDegrees(x.rx), Math.toDegrees(x.ry), Math.toDegrees(x.rz), x.px, x.py, x.pz);
                });
            } else System.out.println("no iron_sword attachable");
        }
    }
}
