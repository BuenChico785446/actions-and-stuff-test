package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.*;
import dev.bedrockmotion.molang.WorldProbe;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.*;

public final class ToolbeltTest {
    public static void main(String[] args) throws Exception {
        try (PackFiles baseFiles = PackFiles.open(Path.of(args[0])); PackFiles addonFiles = PackFiles.open(Path.of(args[1]))) {
            BedrockPack base = BedrockPack.load("base_vanilla", baseFiles, null);
            BedrockPack addon = BedrockPack.load("addon", addonFiles, null);
            base.baseOnly = true;
            BedrockPack.stack(List.of(base, addon));
            EntityStack.Result r = EntityStack.build(List.of(base, addon));
            Map<Integer, String> hotbar = Map.of(2, "minecraft:iron_sword", 4, "minecraft:bow", 6, "minecraft:shield", 8, "minecraft:diamond_pickaxe");
            WorldProbe probe = new WorldProbe() {
                @Override
                public String equippedItemName(String slot, int index) {
                    return slot.equals("slot.hotbar") ? hotbar.getOrDefault(index, "") : "";
                }
            };
            EntityAnimator an = new EntityAnimator(r.entities().get("minecraft:player"));
            an.env.probe = probe;
            for (int f = 0; f < 10; f++) an.update(0.05, f * 0.05, 0.5);
            RenderPlan plan = RenderPlanner.plan(an);
            int visible = 0;
            for (RenderPlan.Layer l : plan.layers) {
                System.out.println("  " + l.geometry.id + "  texture=" + l.texturePath);
                if (!l.geometry.id.endsWith(".blank")) visible++;
            }
            System.out.println(visible >= 4 ? "toolbelt OK (" + visible + " item layers)" : "toolbelt FAILED");
        }
    }
}
