package dev.bedrockmotion.pack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public interface PackFiles extends AutoCloseable {

    List<String> list();

    byte[] read(String path) throws IOException;

    boolean exists(String path);

    @Override
    default void close() throws IOException {}

    static PackFiles open(Path path) throws IOException {
        PackFiles raw = Files.isDirectory(path) ? new DirFiles(path) : new ZipFiles(path);
        return stripWrapper(raw);
    }

    private static PackFiles stripWrapper(PackFiles raw) {
        if (raw.exists("manifest.json")) return raw;
        for (String p : raw.list()) {
            if (p.endsWith("/manifest.json") && p.indexOf('/') == p.lastIndexOf('/')) {
                return new Prefixed(raw, p.substring(0, p.length() - "manifest.json".length()));
            }
        }
        return raw;
    }

    final class Prefixed implements PackFiles {
        private final PackFiles inner;
        private final String prefix;

        public Prefixed(PackFiles inner, String prefix) {
            this.inner = inner;
            this.prefix = prefix;
        }

        @Override
        public List<String> list() {
            List<String> out = new ArrayList<>();
            for (String p : inner.list()) {
                if (p.regionMatches(true, 0, prefix, 0, prefix.length())) out.add(p.substring(prefix.length()));
            }
            return out;
        }

        @Override
        public byte[] read(String path) throws IOException {
            return inner.read(prefix + path);
        }

        @Override
        public boolean exists(String path) {
            return inner.exists(prefix + path);
        }

        @Override
        public void close() throws IOException {
            inner.close();
        }
    }

    final class Overlay implements PackFiles {
        private final PackFiles base;
        private final PackFiles over;
        private final Set<String> names = new LinkedHashSet<>();

        public Overlay(PackFiles base, String overlayPrefix) {
            this.base = base;
            this.over = new Prefixed(base, overlayPrefix);
            for (String p : base.list()) if (!p.regionMatches(true, 0, "subpacks/", 0, 9)) names.add(p);
            names.addAll(over.list());
        }

        @Override
        public List<String> list() {
            return new ArrayList<>(names);
        }

        @Override
        public byte[] read(String path) throws IOException {
            return over.exists(path) ? over.read(path) : base.read(path);
        }

        @Override
        public boolean exists(String path) {
            return over.exists(path) || base.exists(path);
        }

        @Override
        public void close() throws IOException {
            base.close();
        }
    }

    final class DirFiles implements PackFiles {
        private final Path root;
        private final Map<String, Path> index = new HashMap<>();
        private final List<String> names = new ArrayList<>();

        DirFiles(Path root) throws IOException {
            this.root = root;
            try (Stream<Path> s = Files.walk(root)) {
                s.filter(Files::isRegularFile).forEach(p -> {
                    String rel = root.relativize(p).toString().replace('\\', '/');
                    names.add(rel);
                    index.put(rel.toLowerCase(Locale.ROOT), p);
                });
            }
        }

        @Override
        public List<String> list() {
            return names;
        }

        @Override
        public byte[] read(String path) throws IOException {
            Path p = index.get(path.toLowerCase(Locale.ROOT));
            if (p == null) throw new IOException("not found: " + path);
            return Files.readAllBytes(p);
        }

        @Override
        public boolean exists(String path) {
            return index.containsKey(path.toLowerCase(Locale.ROOT));
        }
    }

    final class ZipFiles implements PackFiles {
        private final ZipFile zip;
        private final Map<String, ZipEntry> index = new HashMap<>();
        private final List<String> names = new ArrayList<>();

        ZipFiles(Path file) throws IOException {
            this.zip = new ZipFile(file.toFile());
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                String name = e.getName().replace('\\', '/');
                names.add(name);
                index.put(name.toLowerCase(Locale.ROOT), e);
            }
        }

        @Override
        public List<String> list() {
            return names;
        }

        @Override
        public byte[] read(String path) throws IOException {
            ZipEntry e = index.get(path.toLowerCase(Locale.ROOT));
            if (e == null) throw new IOException("not found: " + path);
            synchronized (zip) {
                try (InputStream in = zip.getInputStream(e)) {
                    return in.readAllBytes();
                }
            }
        }

        @Override
        public boolean exists(String path) {
            return index.containsKey(path.toLowerCase(Locale.ROOT));
        }

        @Override
        public void close() throws IOException {
            zip.close();
        }
    }
}
