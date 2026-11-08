package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.pack.PackFiles;
import dev.bedrockmotion.tools.PackConverter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConverterTest {

    public static void main(String[] args) throws Exception {
        Path in = Path.of(args[0]);
        Path out = Files.createTempFile("bm-converted", ".zip");
        long t0 = System.currentTimeMillis();
        int n = PackConverter.convert(in, out);
        System.out.println("Converted: " + n + " files in " + (System.currentTimeMillis() - t0) + " ms, " + Files.size(out) / 1024 + " KiB");
        BedrockPack direct;
        try (PackFiles f = PackFiles.open(in)) {
            List<String> subs = BedrockPack.subpacks(f);
            direct = BedrockPack.load("direct", f, subs.isEmpty() ? null : subs.get(subs.size() - 1));
        }
        try (PackFiles raw = PackFiles.open(out)) {
            long oggs = raw.list().stream().filter(x -> x.startsWith("assets/bedrockmotion/sounds/")).count();
            boolean soundsJson = raw.exists("assets/bedrockmotion/sounds.json");
            System.out.println("Sounds: sounds.json=" + soundsJson + ", ogg files=" + oggs);
            long blocks = raw.list().stream().filter(x -> x.startsWith("assets/minecraft/textures/block/") && x.endsWith(".png")).count();
            long items = raw.list().stream().filter(x -> x.startsWith("assets/minecraft/textures/item/")).count();
            long metas = raw.list().stream().filter(x -> x.endsWith(".png.mcmeta")).count();
            long colormaps = raw.list().stream().filter(x -> x.startsWith("assets/minecraft/textures/colormap/")).count();
            long env = raw.list().stream().filter(x -> x.startsWith("assets/minecraft/textures/environment/")).count();
            long langs = raw.list().stream().filter(x -> x.startsWith("assets/bedrockmotion/lang/")).count();
            System.out.println("Java assets: block=" + blocks + " item=" + items + " flipbook-meta=" + metas + " colormap=" + colormaps
                    + " environment=" + env + " lang=" + langs);
            for (String x : raw.list()) {
                if (x.endsWith(".png.mcmeta") || x.startsWith("assets/bedrockmotion/lang/")) {
                    dev.bedrockmotion.json.Json.parse(new String(raw.read(x), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            if (soundsJson) {
                Object j = dev.bedrockmotion.json.Json.parse(new String(raw.read("assets/bedrockmotion/sounds.json"), java.nio.charset.StandardCharsets.UTF_8));
                System.out.println("Sound events in sounds.json: " + dev.bedrockmotion.json.Json.obj(j).size());
            }
            PackFiles view = new PackFiles.Prefixed(raw, "assets/bedrockmotion/bedrockmotion/");
            List<String> subs = BedrockPack.subpacks(view);
            BedrockPack viaJava = BedrockPack.load("java", view, subs.isEmpty() ? null : subs.get(subs.size() - 1));
            System.out.println("direct:    " + direct.summary());
            System.out.println("converted: " + viaJava.summary());
            if (!direct.summary().equals(viaJava.summary())) System.exit(1);
            boolean tex = viaJava.textureBytes("textures/entity/cow") != null || viaJava.entities.isEmpty()
                    || direct.entities.values().stream().anyMatch(d -> !d.textures.isEmpty());
            System.out.println("Conversion ok: " + tex);
        }
        Files.deleteIfExists(out);
    }
}
