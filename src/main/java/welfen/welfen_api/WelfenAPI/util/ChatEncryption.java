package welfen.welfen_api.WelfenAPI.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChatEncryption {

    private static final String KEY = "1234567890123456"; // 16 Zeichen = 128 Bit AES
    private static final String ALGORITHM = "AES";

    public static void saveEncrypted(Path path, String content) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(content.getBytes());
        Files.write(path, encrypted);
    }

    public static String loadDecrypted(Path path) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decrypted = cipher.doFinal(Files.readAllBytes(path));
        return new String(decrypted);
    }
}
