public class VerifyTOTPUtil {

    public static boolean verify(String hexSeed, String code) {
        try {
            long currentWindow = System.currentTimeMillis() / 1000L / 30L;

            // Accept previous, current, and next window (±1)
            for (long w = currentWindow - 1; w <= currentWindow + 1; w++) {
                String expected = TOTP.generateTOTP(hexSeed, w, 6);
                if (expected.equals(code)) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
