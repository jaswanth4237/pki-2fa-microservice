import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class DecryptSeedUtil {

   private static final String PRIVATE_KEY_PATH = "/app/student_private.pem";


    static {
        // Add BouncyCastle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String decrypt(String encryptedBase64) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);

            String pem = new String(Files.readAllBytes(Paths.get(PRIVATE_KEY_PATH)));
            pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                     .replace("-----END PRIVATE KEY-----", "")
                     .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(pem);

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);

            // BouncyCastle OAEP SHA-256/MGF1(SHA-256)
            Cipher cipher = Cipher.getInstance("RSA/None/OAEPWITHSHA256ANDMGF1PADDING", "BC");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] decrypted = cipher.doFinal(encryptedBytes);

            return new String(decrypted).trim().toLowerCase();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
