import java.util.HashMap;
import java.util.Map;

public class GenerateTOTPUtil {

    public static Map<String, Object> generate(String hexSeed) {
        Map<String, Object> response = new HashMap<>();

        try {
            long timeWindow = System.currentTimeMillis() / 1000L / 30L;
            String code = TOTP.generateTOTP(hexSeed, timeWindow, 6);

            response.put("code", code);
            response.put("valid_for", 30 - (System.currentTimeMillis() / 1000L % 30));

        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Generation failed");
        }

        return response;
    }
}
