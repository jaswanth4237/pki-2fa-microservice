import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Base64;
import javax.crypto.Cipher;

public class CommitProof {

    // ---------- MAIN ----------
    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.err.println("Usage: java CommitProof <commit-hash>");
                System.exit(1);
            }

            String commitHash = args[0].trim();
            if (commitHash.length() != 40) {
                System.err.println("Commit hash must be 40-character hex string.");
                System.exit(1);
            }

            // 1. Load keys
            PrivateKey studentPrivate = loadPrivateKey("student_private.pem");
            PublicKey instructorPublic = loadPublicKey("instructor_public.pem");

            // 2. Sign commit hash with RSA-PSS + SHA-256 (max salt length)
            byte[] signature = signCommitHash(commitHash, studentPrivate);

            // 3. Encrypt signature with instructor public key using RSA/OAEP + SHA-256
            byte[] encryptedSignature = encryptForInstructor(signature, instructorPublic);

            // 4. Base64-encode (single line)
            String encryptedSignatureB64 = Base64.getEncoder().encodeToString(encryptedSignature);

            System.out.println("Commit Hash:");
            System.out.println(commitHash);
            System.out.println();
            System.out.println("Encrypted Signature (Base64, single line):");
            System.out.println(encryptedSignatureB64);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // ---------- LOAD KEYS FROM PEM ----------

    private static PrivateKey loadPrivateKey(String pemPath) throws Exception {
        String pem = new String(Files.readAllBytes(Paths.get(pemPath)), StandardCharsets.UTF_8);
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private static PublicKey loadPublicKey(String pemPath) throws Exception {
        String pem = new String(Files.readAllBytes(Paths.get(pemPath)), StandardCharsets.UTF_8);
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    // ---------- SIGN COMMIT HASH WITH RSA-PSS ----------

    private static byte[] signCommitHash(String commitHash, PrivateKey privateKey) throws Exception {
        // Sign ASCII string of commit hash, NOT binary
        byte[] messageBytes = commitHash.getBytes(StandardCharsets.UTF_8);

        Signature sig = Signature.getInstance("RSASSA-PSS");

       int maxSaltLen = 478;

PSSParameterSpec pssSpec = new PSSParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA256,
        maxSaltLen,
        PSSParameterSpec.TRAILER_FIELD_BC
);
        sig.setParameter(pssSpec);

        sig.initSign(privateKey);
        sig.update(messageBytes);
        return sig.sign();
    }

    // ---------- ENCRYPT SIGNATURE WITH RSA/OAEP ----------

    private static byte[] encryptForInstructor(byte[] data, PublicKey publicKey) throws Exception {
        // RSA/OAEP with SHA-256 + MGF1(SHA-256)
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }
}
