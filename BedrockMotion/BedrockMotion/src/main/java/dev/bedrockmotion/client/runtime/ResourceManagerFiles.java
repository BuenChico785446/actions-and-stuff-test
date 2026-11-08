package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.pack.PackFiles;
import dev.bedrockmotion.pack.PathMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

final class ResourceManagerFiles implements PackFiles {

    static final String ROOT = "bedrockmotion/";

    private final ResourceManager manager;
    private final Map<String, Identifier> index = new HashMap<>();
    private final List<String> names = new ArrayList<>();

    ResourceManagerFiles(ResourceManager manager) {
        this.manager = manager;
        for (Identifier id : manager.listResources("bedrockmotion", i -> true).keySet()) {
            String path = id.getPath();
            if (!path.startsWith(ROOT)) continue;
            String rel = path.substring(ROOT.length());
            names.add(rel);
            index.put(rel.toLowerCase(Locale.ROOT), id);
        }
    }

    boolean isEmpty() {
        return names.isEmpty();
    }

    @Override
    public List<String> list() {
        return names;
    }

    @Override
    public byte[] read(String path) throws IOException {
        Identifier id = index.get(PathMapper.sanitize(path));
        if (id == null) throw new IOException("not found: " + path);
        try (InputStream in = manager.getResourceOrThrow(id).open()) {
            return in.readAllBytes();
        }
    }

    @Override
    public boolean exists(String path) {
        return index.containsKey(PathMapper.sanitize(path));
    }
}
