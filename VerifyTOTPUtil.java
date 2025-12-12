import java.util.Map;
import java.util.HashMap;

public class VerifyTOTPUtil {

    public static boolean verify(String hexSeed, String code) {
        try {
            int otpLength = code.length();

            // 30-second time window
            long w = System.currentTimeMillis() / 30000;

            // Generate expected TOTP for this window
            String expected = TOTP.generateTOTP(hexSeed, w, otpLength);

            return expected.equals(code);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
