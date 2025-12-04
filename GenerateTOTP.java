import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class GenerateTOTP {

    public static void main(String[] args) throws Exception {

        // 1. Read the 64-character hex seed
        String hexSeed = Files.readString(Paths.get("seed.txt")).trim();

        // Convert hex → bytes
        byte[] seedBytes = hexToBytes(hexSeed);

        // 2. Generate TOTP for current time (30-second window)
        long timeStep = Instant.now().getEpochSecond() / 30;

        String totp = generateTOTP(seedBytes, timeStep);

        // 3. Print TOTP
        System.out.println("Current TOTP: " + totp);

        // 4. Remaining seconds in this 30s window
        long remaining = 30 - (Instant.now().getEpochSecond() % 30);
        System.out.println("Valid for (seconds): " + remaining);
    }

    // Convert hex string → byte array
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] =
                (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                      + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    // Generate 6-digit TOTP
    private static String generateTOTP(byte[] key, long timeStep) throws Exception {
        // Convert time step to 8-byte array
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(timeStep);
        byte[] timeBytes = buffer.array();

        // HMAC-SHA1(key, timeBytes)
        Mac hmac = Mac.getInstance("HmacSHA1");
        SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA1");
        hmac.init(keySpec);

        byte[] hash = hmac.doFinal(timeBytes);

        // Dynamic truncation
        int offset = hash[hash.length - 1] & 0xf;
        int binary =
                ((hash[offset] & 0x7f) << 24)
              | ((hash[offset + 1] & 0xff) << 16)
              | ((hash[offset + 2] & 0xff) << 8)
              | (hash[offset + 3] & 0xff);

        // 6-digit code
        int otp = binary % 1_000_000;

        return String.format("%06d", otp);
    }
}
