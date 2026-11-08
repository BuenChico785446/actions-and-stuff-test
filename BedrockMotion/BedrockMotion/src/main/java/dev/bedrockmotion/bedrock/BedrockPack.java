package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.molang.Symbols;
import dev.bedrockmotion.pack.MaterialKind;
import dev.bedrockmotion.pack.PackFiles;
import dev.bedrockmotion.pack.SoundDefs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BedrockPack {

    public final String name;
    public final PackFiles files;
    public final Symbols symbols = new Symbols();
    public final Map<String, GeometryDef> geometries = new ConcurrentHashMap<>();
    public final Map<String, AnimationDef> animations = new ConcurrentHashMap<>();
    public final Map<String, ControllerDef> controllers = new ConcurrentHashMap<>();
    public final Map<String, RenderControllerDef> renderControllers = new ConcurrentHashMap<>();
    public final Map<String, ClientEntityDef> entities = new ConcurrentHashMap<>();
    public final Map<String, ClientEntityDef> attachables = new ConcurrentHashMap<>();
    public final Map<String, ParticleDef> particles = new ConcurrentHashMap<>();
    public final Map<String, String> materialBase = new ConcurrentHashMap<>();
    public final List<String> problems = Collections.synchronizedList(new ArrayList<>());
    public final java.util.Set<String> soundEvents = ConcurrentHashMap.newKeySet();
    public PackSettingsData settings = new PackSettingsData();
    public boolean javaResourcePack;
    private final Map<String, MaterialKind> kindCache = new ConcurrentHashMap<>();

    private BedrockPack(String name, PackFiles files) {
        this.name = name;
        this.files = files;
    }

    public static List<String> subpacks(PackFiles files) {
        List<String> out = new ArrayList<>();
        try {
            if (!files.exists("manifest.json")) return out;
            Map<String, Object> m = Json.obj(Json.parse(new String(files.read("manifest.json"), StandardCharsets.UTF_8)));
            List<Object> subs = m == null ? null : Json.arr(m.get("subpacks"));
            if (subs != null) {
                for (Object o : subs) {
                    Map<String, Object> s = Json.obj(o);
                    if (s != null && s.get("folder_name") instanceof String f) out.add(f);
                }
            }
        } catch (IOException | RuntimeException ignored) {
            return out;
        }
        return out;
    }

    public static PackFiles effectiveFiles(PackFiles files, String subpack) {
        String overlay = subpack == null ? "" : "subpacks/" + subpack + "/";
        return subpack == null || subpack.isEmpty()
                || files.list().stream().noneMatch(p -> p.regionMatches(true, 0, overlay, 0, overlay.length()))
                ? files
                : new PackFiles.Overlay(files, overlay);
    }

    public static BedrockPack load(String name, PackFiles files, String subpack) {
        BedrockPack pack = new BedrockPack(name, effectiveFiles(files, subpack));
        pack.loadAll();
        return pack;
    }

    private void loadAll() {
        try {
            if (files.exists("manifest.json")) {
                settings = PackSettingsData.fromManifest(Json.parse(text("manifest.json")));
            }
        } catch (IOException | RuntimeException ex) {
            problems.add("manifest.json: " + ex.getMessage());
        }
        List<String> all = files.list();
        all.parallelStream().forEach(path -> {
            String lower = path.toLowerCase(Locale.ROOT);
            try {
                if (lower.endsWith(".material")) {
                    loadMaterials(path);
                    return;
                }
                if (!lower.endsWith(".json")) return;
                if (lower.equals("sounds/sound_definitions.json")) {
                    soundEvents.addAll(SoundDefs.parse(json(path)).keySet());
                } else if (lower.startsWith("animations/")) {
                    animations.putAll(AnimationDef.parse(json(path), symbols, tagged(path)));
                } else if (lower.startsWith("animation_controllers/")) {
                    controllers.putAll(ControllerDef.parse(json(path), symbols, tagged(path)));
                } else if (lower.startsWith("particles/")) {
                    ParticleDef pd = ParticleDef.parse(json(path), symbols, tagged(path));
                    if (pd != null) particles.put(pd.id, pd);
                } else if (lower.startsWith("render_controllers/")) {
                    renderControllers.putAll(RenderControllerDef.parse(json(path), symbols, tagged(path)));
                } else if (lower.startsWith("models/")) {
                    for (GeometryDef g : GeometryDef.parse(json(path), tagged(path))) {
                        geometries.put(g.id.toLowerCase(Locale.ROOT), g);
                    }
                } else if (lower.startsWith("entity/") || lower.startsWith("attachables/")) {
                    ClientEntityDef def = ClientEntityDef.parse(json(path), symbols, tagged(path));
                    if (def != null) {
                        def.pack = this;
                        (def.attachable ? attachables : entities).merge(def.identifier, def, BedrockPack::newer);
                    }
                }
            } catch (IOException | RuntimeException ex) {
                problems.add(path + ": " + ex.getMessage());
            }
        });
    }

    private List<String> tagged(String path) {
        return new ArrayList<>() {
            @Override
            public boolean add(String s) {
                return problems.add(path + ": " + s);
            }
        };
    }

    private String text(String path) throws IOException {
        return new String(files.read(path), StandardCharsets.UTF_8);
    }

    private Object json(String path) throws IOException {
        return Json.parse(text(path));
    }

    private void loadMaterials(String path) throws IOException {
        Map<String, Object> root = Json.obj(Json.parse(text(path)));
        Map<String, Object> mats = root == null ? null : Json.obj(root.get("materials"));
        if (mats == null) return;
        for (String key : mats.keySet()) {
            int c = key.indexOf(':');
            if (c > 0) materialBase.put(key.substring(0, c).toLowerCase(Locale.ROOT), key.substring(c + 1).toLowerCase(Locale.ROOT));
        }
    }

    public MaterialKind materialKind(String material) {
        if (material == null) return MaterialKind.CUTOUT;
        String key = material.toLowerCase(Locale.ROOT);
        return kindCache.computeIfAbsent(key, k -> {
            String cur = k;
            StringBuilder chain = new StringBuilder(k);
            for (int i = 0; i < 12; i++) {
                String base = materialBase.get(cur);
                if (base == null) break;
                chain.append(' ').append(base);
                cur = base;
            }
            String s = chain.toString();
            if (s.contains("emissive")) return MaterialKind.EMISSIVE;
            if (s.contains("eyes") || s.contains("beacon") || s.contains("spider")) return MaterialKind.EYES;
            if (s.contains("alphablend") || s.contains("alpha_blend") || s.contains("translucent") || s.contains("blend")) {
                return MaterialKind.TRANSLUCENT;
            }
            return MaterialKind.CUTOUT;
        });
    }

    private static ClientEntityDef newer(ClientEntityDef a, ClientEntityDef b) {
        if (a.minEngine != b.minEngine) return a.minEngine > b.minEngine ? a : b;
        return a;
    }

    public boolean isMultitexture(String material) {
        if (material == null) return false;
        String cur = material.toLowerCase(Locale.ROOT);
        for (int i = 0; i < 12 && cur != null; i++) {
            if (cur.contains("multitexture")) return true;
            cur = materialBase.get(cur);
        }
        return false;
    }

    public byte[] textureBytes(String ref) {
        if (ref == null) return null;
        String base = ref.endsWith(".png") || ref.endsWith(".tga") ? ref.substring(0, ref.length() - 4) : ref;
        for (String ext : new String[] {".png", ".tga", ".jpg", ".jpeg"}) {
            if (files.exists(base + ext)) {
                try {
                    return files.read(base + ext);
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return null;
    }

    public static boolean isTga(byte[] data) {
        return data.length > 2 && !(data[0] == (byte) 0x89 && data[1] == 'P');
    }

    public Map<String, String> summary() {
        Map<String, String> s = new HashMap<>();
        s.put("geometries", String.valueOf(geometries.size()));
        s.put("animations", String.valueOf(animations.size()));
        s.put("controllers", String.valueOf(controllers.size()));
        s.put("renderControllers", String.valueOf(renderControllers.size()));
        s.put("entities", String.valueOf(entities.size()));
        s.put("attachables", String.valueOf(attachables.size()));
        s.put("particles", String.valueOf(particles.size()));
        s.put("materials", String.valueOf(materialBase.size()));
        s.put("settings", String.valueOf(settings.size()));
        s.put("problems", String.valueOf(problems.size()));
        return s;
    }
}
