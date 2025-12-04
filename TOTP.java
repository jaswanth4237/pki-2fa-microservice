import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TOTP {

    public static String generateTOTP(String hexKey, long timeWindow, int digits) throws Exception {
        byte[] keyBytes = hexStringToByteArray(hexKey);

        byte[] timeBytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            timeBytes[i] = (byte) (timeWindow & 0xFF);
            timeWindow >>= 8;
        }

        SecretKeySpec signKey = new SecretKeySpec(keyBytes, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);

        byte[] hash = mac.doFinal(timeBytes);

        int offset = hash[hash.length - 1] & 0xF;

        int binary =
                ((hash[offset] & 0x7F) << 24) |
                ((hash[offset + 1] & 0xFF) << 16) |
                ((hash[offset + 2] & 0xFF) << 8) |
                (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, digits);

        return String.format("%0" + digits + "d", otp);
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));

        return data;
    }
}
