package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.*;
import dev.bedrockmotion.molang.EntitySnapshot;
import dev.bedrockmotion.molang.WorldProbe;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.*;

public final class PoseTrace {
    public static void main(String[] args) throws Exception {
        String entity = args[2];
        double yawSpeed = Double.parseDouble(args[3]);
        double moveSpeed = Double.parseDouble(args[4]);
        int frames = Integer.parseInt(args[5]);
        Set<String> bones = new HashSet<>(Arrays.asList(args[6].split(",")));
        String item = args.length > 7 ? args[7] : "";
        boolean attack = args.length > 8 && args[8].equals("attack");
        WorldProbe probe = new WorldProbe() {
            @Override
            public String equippedItemName(String slot) {
                return slot.equals("slot.weapon.mainhand") ? item : "";
            }

            @Override
            public boolean equippedItemHasAnyTag(String slot, String[] tags) {
                if (!slot.equals("slot.weapon.mainhand") || item.isEmpty()) return false;
                String kind = item.substring(item.indexOf('_') + 1);
                for (String tag : tags) if (tag.endsWith("is_" + kind)) return true;
                return false;
            }
        };
        try (PackFiles baseFiles = PackFiles.open(Path.of(args[0])); PackFiles addonFiles = PackFiles.open(Path.of(args[1]))) {
            BedrockPack base = BedrockPack.load("base_vanilla", baseFiles, null);
            BedrockPack addon = BedrockPack.load("addon", addonFiles, null);
            base.baseOnly = true;
            BedrockPack.stack(List.of(base, addon));
            EntityStack.Result r = EntityStack.build(List.of(base, addon));
            EntityAnimator an = new EntityAnimator(r.entities().get(entity));
            an.env.probe = probe;
            double walked = 0;
            for (int f = 0; f < frames; f++) {
                EntitySnapshot s = an.env.entity;
                s.resetEngineVars();
                if (attack) s.engineVars.put("attack_time", (f % 8) / 8.0);
                s.yawSpeed = yawSpeed;
                s.groundSpeed = moveSpeed * 4.3;
                s.modifiedMoveSpeed = moveSpeed;
                walked += moveSpeed * 0.05 * 20 * 0.5;
                s.modifiedDistanceMoved = walked;
                s.moving = moveSpeed > 0;
                s.onGround = true;
                s.alive = true;
                s.health = s.maxHealth = 100;
                an.update(0.05, f * 0.05, 0.5);
                StringBuilder line = new StringBuilder(String.format(Locale.ROOT, "t=%4.2f", f * 0.05));
                an.pose.forEachTouched((name, x) -> {
                    if (bones.contains(name) && !x.isIdentity()) {
                        line.append(String.format(Locale.ROOT, " | %s r[%.0f %.0f %.0f] p[%.1f %.1f %.1f]", name, Math.toDegrees(x.rx), Math.toDegrees(x.ry), Math.toDegrees(x.rz), x.px, x.py, x.pz));
                    }
                });
                if (f % 2 == 0) System.out.println(line);
            }
        }
    }
}
