import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class DecryptSeedUtil {

    private static final String PRIVATE_KEY_FILE = "student_private.pem";

    static {
        // Ensure BouncyCastle is available (class present in included JAR)
        try {
            Security.addProvider(new BouncyCastleProvider());
        } catch (Exception ignored) {}
    }

    public static String decrypt(String encryptedSeedB64) throws Exception {
        if (encryptedSeedB64 == null || encryptedSeedB64.isBlank()) return null;

        byte[] cipherBytes = Base64.getDecoder().decode(encryptedSeedB64.trim());

        PrivateKey privateKey = loadPrivateKeyFromPem(Paths.get(PRIVATE_KEY_FILE));
        if (privateKey == null) throw new IllegalStateException("Private key not loaded");

        // RSA/OAEP with SHA-256 and MGF1(SHA-256)
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                new MGF1ParameterSpec("SHA-256"),
                PSource.PSpecified.DEFAULT
        );
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);

        byte[] plain = cipher.doFinal(cipherBytes);
        String hex = new String(plain, StandardCharsets.UTF_8).trim();

        // validate 64-char hex (case-insensitive)
        if (hex.length() != 64 || !hex.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("Decrypted seed is not a 64-character hex string");
        }

        return hex.toLowerCase();
    }

    private static PrivateKey loadPrivateKeyFromPem(Path pemPath) throws IOException, Exception {
        byte[] all = Files.readAllBytes(pemPath);
        String pem = new String(all, StandardCharsets.UTF_8);
        // remove PEM header/footers and newlines
        pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                 .replace("-----END PRIVATE KEY-----", "")
                 .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }
}
