package dev.bedrockmotion.pack;

public final class TgaDecoder {

    public record Image(int width, int height, int[] argb) {}

    private TgaDecoder() {}

    public static Image decode(byte[] d) {
        if (d.length < 18) throw new IllegalArgumentException("TGA too short");
        int idLen = d[0] & 0xFF;
        int colorMapType = d[1] & 0xFF;
        int type = d[2] & 0xFF;
        int cmLen = (d[5] & 0xFF) | ((d[6] & 0xFF) << 8);
        int cmDepth = d[7] & 0xFF;
        int w = (d[12] & 0xFF) | ((d[13] & 0xFF) << 8);
        int h = (d[14] & 0xFF) | ((d[15] & 0xFF) << 8);
        int bpp = d[16] & 0xFF;
        int desc = d[17] & 0xFF;
        boolean topDown = (desc & 0x20) != 0;
        boolean rle = type == 9 || type == 10 || type == 11;
        int base = type & 7;
        if (base != 2 && base != 3 && base != 1) throw new IllegalArgumentException("TGA-Typ " + type + " not supported");
        int pos = 18 + idLen;
        int[] palette = null;
        if (colorMapType == 1) {
            int entry = (cmDepth + 7) / 8;
            palette = new int[cmLen];
            for (int i = 0; i < cmLen; i++) palette[i] = pixel(d, pos + i * entry, cmDepth);
            pos += cmLen * entry;
        }
        int bytes = (bpp + 7) / 8;
        int[] out = new int[w * h];
        int count = 0;
        int total = w * h;
        byte[] px = new byte[4];
        while (count < total && pos < d.length) {
            int run = 1;
            boolean repeat = false;
            if (rle) {
                int head = d[pos++] & 0xFF;
                run = (head & 0x7F) + 1;
                repeat = (head & 0x80) != 0;
            }
            for (int i = 0; i < run && count < total; i++) {
                if (i == 0 || !repeat) {
                    if (pos + bytes > d.length) break;
                    System.arraycopy(d, pos, px, 0, bytes);
                    pos += bytes;
                }
                int v;
                if (base == 1) {
                    int idx = px[0] & 0xFF;
                    v = palette != null && idx < palette.length ? palette[idx] : 0;
                } else if (base == 3) {
                    int g = px[0] & 0xFF;
                    v = 0xFF000000 | (g << 16) | (g << 8) | g;
                } else {
                    v = pixel(px, 0, bpp);
                }
                out[count++] = v;
            }
        }
        if (!topDown) {
            for (int y = 0; y < h / 2; y++) {
                for (int x = 0; x < w; x++) {
                    int a = y * w + x;
                    int b = (h - 1 - y) * w + x;
                    int t = out[a];
                    out[a] = out[b];
                    out[b] = t;
                }
            }
        }
        return new Image(w, h, out);
    }

    private static int pixel(byte[] d, int p, int bpp) {
        switch (bpp) {
            case 32:
                return ((d[p + 3] & 0xFF) << 24) | ((d[p + 2] & 0xFF) << 16) | ((d[p + 1] & 0xFF) << 8) | (d[p] & 0xFF);
            case 24:
                return 0xFF000000 | ((d[p + 2] & 0xFF) << 16) | ((d[p + 1] & 0xFF) << 8) | (d[p] & 0xFF);
            case 16:
            case 15: {
                int v = (d[p] & 0xFF) | ((d[p + 1] & 0xFF) << 8);
                int r = ((v >> 10) & 31) * 255 / 31;
                int g = ((v >> 5) & 31) * 255 / 31;
                int b = (v & 31) * 255 / 31;
                int a = bpp == 16 && (v & 0x8000) == 0 ? 0 : 255;
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
            default:
                return 0xFF000000 | ((d[p] & 0xFF) << 16) | ((d[p] & 0xFF) << 8) | (d[p] & 0xFF);
        }
    }
}
