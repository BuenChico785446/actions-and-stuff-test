package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.*;
import dev.bedrockmotion.molang.EntitySnapshot;
import dev.bedrockmotion.pack.PackFiles;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

public final class ControllerDump {
    public static void main(String[] args) throws Exception {
        try (PackFiles baseFiles = PackFiles.open(Path.of(args[0])); PackFiles addonFiles = PackFiles.open(Path.of(args[1]))) {
            BedrockPack base = BedrockPack.load("base_vanilla", baseFiles, null);
            BedrockPack addon = BedrockPack.load("addon", addonFiles, null);
            base.baseOnly = true;
            BedrockPack.stack(List.of(base, addon));
            EntityStack.Result r = EntityStack.build(List.of(base, addon));
            ClientEntityDef def = r.entities().get(args[2]);
            EntityAnimator an = new EntityAnimator(def);
            for (int f = 0; f < 40; f++) {
                EntitySnapshot s = an.env.entity;
                s.groundSpeed = 4; s.modifiedMoveSpeed = 4; s.modifiedDistanceMoved += 0.2; s.moving = true; s.onGround = true;
                an.update(0.05, f * 0.05, 0.5);
            }
            Field cf = EntityAnimator.class.getDeclaredField("controllers");
            cf.setAccessible(true);
            for (Map.Entry<?, ?> e : ((Map<?, ?>) cf.get(an)).entrySet()) {
                Object ci = e.getValue();
                Field sf = ci.getClass().getDeclaredField("state");
                sf.setAccessible(true);
                Object st = sf.get(ci);
                String name = "null";
                if (st != null) { Field nf = st.getClass().getDeclaredField("name"); nf.setAccessible(true); name = String.valueOf(nf.get(st)); }
                System.out.println("  " + e.getKey() + " -> " + name);
            }
            for (String v : args.length > 3 ? args[3].split(",") : new String[0]) {
                int id = an.env.symbols.findVar(v);
                System.out.println("  v." + v + " = " + (id < 0 ? "(unused)" : an.env.getVar(id)));
            }
        }
    }
}
