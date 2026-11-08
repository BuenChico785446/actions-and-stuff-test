package dev.bedrockmotion.pack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public final class PngEncoder {

    private PngEncoder() {}

    public static byte[] encode(int width, int height, int[] argb) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(width * height + 256);
            out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
            ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(ihdr);
            d.writeInt(width);
            d.writeInt(height);
            d.write(new byte[] {8, 6, 0, 0, 0});
            chunk(out, "IHDR", ihdr.toByteArray());
            byte[] raw = new byte[(width * 4 + 1) * height];
            int p = 0;
            for (int y = 0; y < height; y++) {
                raw[p++] = 0;
                for (int x = 0; x < width; x++) {
                    int c = argb[y * width + x];
                    raw[p++] = (byte) (c >> 16);
                    raw[p++] = (byte) (c >> 8);
                    raw[p++] = (byte) c;
                    raw[p++] = (byte) (c >>> 24);
                }
            }
            Deflater deflater = new Deflater(6);
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream z = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while (!deflater.finished()) z.write(buf, 0, deflater.deflate(buf));
            deflater.end();
            chunk(out, "IDAT", z.toByteArray());
            chunk(out, "IEND", new byte[0]);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
        DataOutputStream d = new DataOutputStream(out);
        d.writeInt(data.length);
        byte[] t = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        d.write(t);
        d.write(data);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        d.writeInt((int) crc.getValue());
    }

    public static byte[] fromTexture(byte[] data) {
        if (data.length > 8 && data[0] == (byte) 0x89 && data[1] == 'P') return data;
        TgaDecoder.Image img = TgaDecoder.decode(data);
        return encode(img.width(), img.height(), img.argb());
    }
}
