package parsers;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class CharsetDetector {

    private static final int MIN_BOM_WIDTH = 2;
    private static final int MAX_BOM_WIDTH = 5;

    private static final Map<Charset, byte[]> charsetBoms = ImmutableMap.<Charset, byte[]> builder()
            .put(StandardCharsets.UTF_8, new byte[] { (byte) 239, (byte) 187, (byte) 191 })
            .put(StandardCharsets.UTF_16, new byte[] { (byte) 255, (byte) 254 })
            .put(StandardCharsets.UTF_16BE, new byte[] { (byte) 254, (byte) 255 })
            .build();

    public static Charset detect(Path path) {
        Charset charset = StandardCharsets.UTF_8;

        try (InputStream input = new FileInputStream(path.toFile())) {
            final byte[] bom = new byte[MAX_BOM_WIDTH];
            if (input.read(bom) > MIN_BOM_WIDTH) {
                charset = charsetBoms.entrySet().stream()
                        .filter(pair -> matchesBom(bom, pair.getValue()))
                        .map(pair -> pair.getKey())
                        .findFirst()
                        .orElse(StandardCharsets.UTF_8);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return charset;
    }

    private static boolean matchesBom(byte[] fileBom, byte[] typeBom) {
        // We only want to compare up to the *defined BOM* length (typeBom) as the file array
        // might contain actual data from the file... 
        for (int i = 0; i < typeBom.length; i++) {
            if (fileBom[i] != typeBom[i]) {
                return false;
            }
        }

        return true;
    }

}
