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
                if (System.getProperty("swim") != null) s.engineVars.put("swim_amount", Double.parseDouble(System.getProperty("swim")));
                s.yawSpeed = yawSpeed;
                s.groundSpeed = moveSpeed * 4.3;
                s.modifiedMoveSpeed = moveSpeed;
                walked += moveSpeed * 0.05 * 20 * 0.5;
                s.modifiedDistanceMoved = walked;
                s.moving = moveSpeed > 0;
                s.onGround = true;
                s.alive = true;
                s.health = args.length > 9 ? Double.parseDouble(args[9]) : 100; s.maxHealth = 100;
                an.update(0.05, f * 0.05, 0.5);
                if (f == 0 && args.length > 10) an.env.setVar(an.env.symbols.varId("broke"), Double.parseDouble(args[10]));
                StringBuilder line = new StringBuilder(String.format(Locale.ROOT, "t=%4.2f", f * 0.05));
                an.pose.forEachTouched((name, x) -> {
                    if (bones.contains(name) && !x.isIdentity()) {
                        line.append(String.format(Locale.ROOT, " | %s r[%.0f %.0f %.0f] p[%.1f %.1f %.1f]", name, Math.toDegrees(x.rx), Math.toDegrees(x.ry), Math.toDegrees(x.rz), x.px, x.py, x.pz));
                    }
                });
                if (f % 2 == 0) System.out.println(line);
                if (f == frames - 1) {
                    RenderPlan plan = RenderPlanner.plan(an);
                    String flip = System.getProperty("flip", "");
                    if (!flip.isEmpty()) {
                        Pose.Xf rootXf = an.pose.get("root");
                        if (flip.contains("rx")) rootXf.rx = -rootXf.rx;
                        if (flip.contains("pz")) rootXf.pz = -rootXf.pz;
                        if (flip.contains("py")) rootXf.py = -rootXf.py;
                    }
                    String flipAll = System.getProperty("flipAll", "");
                    if (!flipAll.isEmpty()) {
                        an.pose.forEachTouched((name, x) -> {
                            if (flipAll.contains("rx")) x.rx = -x.rx;
                            if (flipAll.contains("ry")) x.ry = -x.ry;
                            if (flipAll.contains("rz")) x.rz = -x.rz;
                            if (flipAll.contains("pz")) x.pz = -x.pz;
                            if (flipAll.contains("py")) x.py = -x.py;
                            if (flipAll.contains("px")) x.px = -x.px;
                        });
                    }
                    if (!plan.layers.isEmpty()) {
                        GeometryDef geo = plan.layers.get(0).geometry;
                        double[][] mats = LocatorSolver.solveAll(geo, an.pose).mats;
                        double min = 1e9;
                        double torsoMin = 1e9;
                        String lowest = "";
                        for (int b = 0; b < geo.bones.length; b++) {
                            double boneMin = 1e9;
                            for (GeometryDef.Cube c : geo.bones[b].cubes) {
                                for (GeometryDef.Quad q : c.quads) {
                                    if (q == null) continue;
                                    for (int v = 0; v < 4; v++) {
                                        double x = q.pos[v * 3] / 16.0, y = q.pos[v * 3 + 1] / 16.0, z = q.pos[v * 3 + 2] / 16.0;
                                        double wy = mats[b][1] * x + mats[b][5] * y + mats[b][9] * z + mats[b][13];
                                        boneMin = Math.min(boneMin, wy);
                                        String bn = geo.bones[b].name.toLowerCase(Locale.ROOT);
                                        if (bn.equals("head") || bn.equals("hat") || bn.equals("body")) torsoMin = Math.min(torsoMin, wy);
                                        if (wy < min) {
                                            min = wy;
                                            lowest = geo.bones[b].name;
                                        }
                                    }
                                }
                            }
                            if (boneMin * 16 < -3) System.out.printf(Locale.ROOT, "  below ground: %s %.1f px%n", geo.bones[b].name, boneMin * 16);
                        }
                        for (String probeBone : new String[] {"head", "body", "rightleg", "rightarm"}) {
                            int bi = geo.indexOf(probeBone);
                            if (bi < 0) continue;
                            GeometryDef.Bone pb = geo.bones[bi];
                            double[] m = mats[bi];
                            double px = pb.pivotX / 16.0, py = pb.pivotY / 16.0, pz = pb.pivotZ / 16.0;
                            System.out.printf(Locale.ROOT, "  %s pivot -> x %.1f y %.1f z %.1f (rest y %.1f z %.1f)%n", probeBone,
                                    (m[0] * px + m[4] * py + m[8] * pz + m[12]) * 16, (m[1] * px + m[5] * py + m[9] * pz + m[13]) * 16,
                                    (m[2] * px + m[6] * py + m[10] * pz + m[14]) * 16, py * 16, pz * 16);
                        }
                        System.out.printf(Locale.ROOT, "torso/head lowest %.2f px%n", torsoMin * 16);
                        System.out.printf(Locale.ROOT, "lowest point %.2f pixels (bone %s)%n", min * 16, lowest);
                    }
                }
            }
        }
    }
}
