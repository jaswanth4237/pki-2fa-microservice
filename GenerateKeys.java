import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class GenerateKeys {
    public static void main(String[] args) throws Exception {

        // Generate 4096-bit RSA key pair
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(4096);
        KeyPair keyPair = generator.generateKeyPair();

        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Convert to PEM format
        String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKey.getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        String publicPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(publicKey.getEncoded())
                + "\n-----END PUBLIC KEY-----\n";

        // Save files
        Files.write(Paths.get("student_private.pem"), privatePem.getBytes());
        Files.write(Paths.get("student_public.pem"), publicPem.getBytes());

        System.out.println("✔ RSA 4096-bit keys generated successfully!");
    }
}
