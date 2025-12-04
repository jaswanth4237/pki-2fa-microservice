import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.Cipher;

public class KeyTest {
    public static void main(String[] args) throws Exception {

        // Load PRIVATE KEY
        String privPem = new String(Files.readAllBytes(Paths.get("student_private.pem")))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] privBytes = java.util.Base64.getDecoder().decode(privPem);
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(privBytes));

        // Load PUBLIC KEY
        String pubPem = new String(Files.readAllBytes(Paths.get("student_public.pem")))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] pubBytes = java.util.Base64.getDecoder().decode(pubPem);
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(pubBytes));

        // Encrypt → Decrypt
        Cipher c1 = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        c1.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = c1.doFinal("Hello".getBytes());

        Cipher c2 = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        c2.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = c2.doFinal(encrypted);

        System.out.println("Decrypted text: " + new String(decrypted));
    }
}
