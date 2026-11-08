package dev.bedrockmotion.tools;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.pack.PackFiles;
import dev.bedrockmotion.pack.PathMapper;
import dev.bedrockmotion.pack.PngEncoder;
import dev.bedrockmotion.pack.SoundDefs;
import dev.bedrockmotion.pack.TextureNames;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class PackConverter {

    private static final String MCMETA = "{\n  \"pack\": {\n    \"description\": \"%s (BedrockMotion)\",\n    \"pack_format\": 88\n  }\n}\n";

    private PackConverter() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: PackConverter <bedrock-pack.mcpack|zip|folder> <output.zip>");
            System.exit(2);
        }
        int n = convert(Path.of(args[0]), Path.of(args[1]));
        System.out.println(n + " files to " + args[1] + " written.");
    }

    public static int convert(Path input, Path output) throws IOException {
        String name = input.getFileName().toString();
        int count = 0;
        Set<String> written = new HashSet<>();
        try (PackFiles files = PackFiles.open(input);
                OutputStream os = Files.newOutputStream(output);
                ZipOutputStream zip = new ZipOutputStream(os)) {
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));
            zip.write(String.format(MCMETA, name.replace("\"", "'")).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (String path : files.list()) {
                if (path.endsWith("/")) continue;
                String target = "assets/bedrockmotion/" + PathMapper.ROOT + PathMapper.sanitize(path);
                if (!written.add(target)) continue;
                zip.putNextEntry(new ZipEntry(target));
                zip.write(files.read(path));
                zip.closeEntry();
                count++;
            }
            List<String> subs = BedrockPack.subpacks(files);
            PackFiles effective = BedrockPack.effectiveFiles(files, subs.isEmpty() ? null : subs.get(subs.size() - 1));
            count += writeSounds(effective, zip, written);
            count += writeAssets(effective, zip, written);
        }
        return count;
    }

    private static final Set<String> ENVIRONMENT = Set.of("sun", "moon_phases", "clouds", "rain", "snow", "end_sky");

    private static boolean put(ZipOutputStream zip, Set<String> written, String target, byte[] data) throws IOException {
        if (!written.add(target)) return false;
        zip.putNextEntry(new ZipEntry(target));
        zip.write(data);
        zip.closeEntry();
        return true;
    }

    private static String stem(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private static boolean isPbr(String stem) {
        return stem.endsWith("_mer") || stem.endsWith("_normal") || stem.endsWith("_heightmap") || stem.endsWith("_mers");
    }

    private static byte[] textureBytes(PackFiles files, String base) throws IOException {
        for (String ext : new String[] {".png", ".tga"}) {
            if (files.exists(base + ext)) return PngEncoder.fromTexture(files.read(base + ext));
        }
        return null;
    }

    private static int writeAssets(PackFiles files, ZipOutputStream zip, Set<String> written) throws IOException {
        int count = 0;
        String root = "assets/minecraft/textures/";
        if (files.exists("textures/flipbook_textures.json")) {
            List<Object> flips = Json.arr(Json.parse(new String(files.read("textures/flipbook_textures.json"), StandardCharsets.UTF_8)));
            if (flips != null) {
                for (Object o : flips) {
                    Map<String, Object> f = Json.obj(o);
                    if (f == null || !(f.get("flipbook_texture") instanceof String strip) || !(f.get("atlas_tile") instanceof String tile)) continue;
                    byte[] png = textureBytes(files, strip);
                    if (png == null) continue;
                    String name = PathMapper.sanitize(TextureNames.block(tile.toLowerCase(Locale.ROOT)));
                    if (put(zip, written, root + "block/" + name + ".png", png)) count++;
                    StringBuilder meta = new StringBuilder("{\"animation\": {\"frametime\": ");
                    meta.append(Math.max(1, (int) Json.num(f.get("ticks_per_frame"), 1)));
                    meta.append(", \"interpolate\": ").append(Json.bool(f.get("blend_frames"), true));
                    List<Object> frames = Json.arr(f.get("frames"));
                    if (frames != null && !frames.isEmpty()) {
                        meta.append(", \"frames\": [");
                        for (int i = 0; i < frames.size(); i++) {
                            if (i > 0) meta.append(", ");
                            Object fr = frames.get(i);
                            if (Json.obj(fr) != null) {
                                meta.append(String.format(Locale.ROOT, "{\"index\": %d, \"time\": %d}",
                                        (int) Json.num(Json.obj(fr).get("index"), 0), Math.max(1, (int) Json.num(Json.obj(fr).get("time"), 1))));
                            } else {
                                meta.append((int) Json.num(fr, 0));
                            }
                        }
                        meta.append("]");
                    }
                    meta.append("}}\n");
                    if (put(zip, written, root + "block/" + name + ".png.mcmeta", meta.toString().getBytes(StandardCharsets.UTF_8))) count++;
                }
            }
        }
        for (String path : files.list()) {
            String lower = path.toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".png") || lower.endsWith(".tga"))) continue;
            String stem = stem(lower);
            if (isPbr(stem)) continue;
            String target = null;
            if (lower.startsWith("textures/blocks/") && lower.indexOf('/', 16) < 0) {
                target = root + "block/" + PathMapper.sanitize(TextureNames.block(stem)) + ".png";
            } else if (lower.startsWith("textures/items/") && lower.indexOf('/', 15) < 0) {
                target = root + "item/" + PathMapper.sanitize(TextureNames.item(stem)) + ".png";
            } else if (lower.startsWith("textures/colormap/") && lower.indexOf('/', 18) < 0) {
                target = root + "colormap/" + PathMapper.sanitize(stem) + ".png";
            } else if (lower.startsWith("textures/environment/") && lower.indexOf('/', 21) < 0 && ENVIRONMENT.contains(stem)) {
                target = root + "environment/" + stem + ".png";
            }
            if (target == null || written.contains(target)) continue;
            if (put(zip, written, target, PngEncoder.fromTexture(files.read(path)))) count++;
        }
        for (String path : files.list()) {
            String lower = path.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("texts/") || !lower.endsWith(".lang")) continue;
            String locale = stem(lower).toLowerCase(Locale.ROOT);
            StringBuilder json = new StringBuilder("{\n");
            boolean first = true;
            for (String line : new String(files.read(path), StandardCharsets.UTF_8).split("\\r?\\n")) {
                int eq = line.indexOf('=');
                if (eq <= 0 || line.startsWith("##")) continue;
                String value = line.substring(eq + 1);
                int comment = value.indexOf("\t#");
                if (comment >= 0) value = value.substring(0, comment);
                if (!first) json.append(",\n");
                first = false;
                json.append("  \"").append(escape(line.substring(0, eq).trim())).append("\": \"").append(escape(value)).append("\"");
            }
            json.append("\n}\n");
            if (put(zip, written, "assets/bedrockmotion/lang/" + locale + ".json", json.toString().getBytes(StandardCharsets.UTF_8))) count++;
        }
        return count;
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                case '\r' -> {}
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int writeSounds(PackFiles effective, ZipOutputStream zip, Set<String> written) throws IOException {
        if (!effective.exists("sounds/sound_definitions.json")) return 0;
        Map<String, List<SoundDefs.Sound>> defs =
                SoundDefs.parse(Json.parse(new String(effective.read("sounds/sound_definitions.json"), StandardCharsets.UTF_8)));
        StringBuilder sb = new StringBuilder("{\n");
        boolean firstEvent = true;
        Set<String> copied = new HashSet<>();
        int count = 0;
        for (Map.Entry<String, List<SoundDefs.Sound>> e : defs.entrySet()) {
            if (!firstEvent) sb.append(",\n");
            firstEvent = false;
            sb.append("  \"").append(PathMapper.sanitize(e.getKey())).append("\": {\"sounds\": [");
            boolean firstSound = true;
            for (SoundDefs.Sound s : e.getValue()) {
                String name = SoundDefs.javaSoundName(s.path());
                if (!firstSound) sb.append(", ");
                firstSound = false;
                sb.append(String.format(Locale.ROOT, "{\"name\": \"bedrockmotion:%s\", \"volume\": %s, \"pitch\": %s}",
                        name, s.volume(), s.pitch()));
                String source = s.path().endsWith(".ogg") ? s.path() : s.path() + ".ogg";
                String target = "assets/bedrockmotion/sounds/" + name + ".ogg";
                if (copied.add(target) && effective.exists(source) && written.add(target)) {
                    zip.putNextEntry(new ZipEntry(target));
                    zip.write(effective.read(source));
                    zip.closeEntry();
                    count++;
                }
            }
            sb.append("]}");
        }
        sb.append("\n}\n");
        zip.putNextEntry(new ZipEntry("assets/bedrockmotion/sounds.json"));
        zip.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        return count + 1;
    }
}
