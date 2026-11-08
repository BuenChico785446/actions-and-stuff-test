package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.pack.PackFiles;
import java.nio.file.Path;
import java.util.List;

public final class PackCheck {

    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            long t0 = System.currentTimeMillis();
            try (PackFiles files = PackFiles.open(Path.of(arg))) {
                List<String> subs = BedrockPack.subpacks(files);
                String sub = subs.isEmpty() ? null : subs.get(subs.size() - 1);
                BedrockPack pack = BedrockPack.load(Path.of(arg).getFileName().toString(), files, sub);
                long t1 = System.currentTimeMillis();
                System.out.println(arg + " (Subpack: " + sub + ") in " + (t1 - t0) + " ms");
                System.out.println("  " + pack.summary());
                int shown = 0;
                for (String p : pack.problems) {
                    if (shown++ < 12) System.out.println("  Problem: " + (p.length() > 220 ? p.substring(0, 220) : p));
                }
            }
        }
    }
}
