package dev.bedrockmotion;

import dev.bedrockmotion.pack.PackFiles;
import dev.bedrockmotion.pack.PngEncoder;
import dev.bedrockmotion.pack.TgaDecoder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class TextureTest {

    public static void main(String[] args) throws Exception {
        int tga = 0;
        int ok = 0;
        int bad = 0;
        try (PackFiles files = PackFiles.open(Path.of(args[0]))) {
            for (String p : files.list()) {
                if (!p.toLowerCase().endsWith(".tga")) continue;
                tga++;
                try {
                    byte[] data = files.read(p);
                    byte[] png = PngEncoder.fromTexture(data);
                    BufferedImage back = ImageIO.read(new ByteArrayInputStream(png));
                    boolean same = back != null && back.getWidth() > 0 && back.getHeight() > 0;
                    if (png == data) {
                        same &= true;
                    } else {
                        TgaDecoder.Image img = TgaDecoder.decode(data);
                        same &= back.getWidth() == img.width() && back.getHeight() == img.height()
                                && back.getRGB(0, 0) == img.argb()[0]
                                && back.getRGB(img.width() - 1, img.height() - 1) == img.argb()[img.argb().length - 1];
                    }
                    if (same) ok++;
                    else bad++;
                } catch (Exception e) {
                    bad++;
                    if (bad < 4) System.out.println("  Error " + p + ": " + e);
                }
            }
        }
        System.out.println("TGA textures: " + tga + ", ok: " + ok + ", failed: " + bad);
        if (bad > 0) System.exit(1);
    }
}
