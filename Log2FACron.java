import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class Log2FACron {

    public static void main(String[] args) {
        try {
            Path seedPath = Paths.get("/data/seed.txt");

            if (!Files.exists(seedPath)) {
                System.err.println("Seed not found at /data/seed.txt");
                return;
            }

            String hexSeed = Files.readString(seedPath).trim();

            // Re-use your GenerateTOTPUtil
            Map<String, Object> result = GenerateTOTPUtil.generate(hexSeed);
            if (result.get("code") == null) {
                System.err.println("Failed to generate TOTP");
                return;
            }

            String code = result.get("code").toString();

            // UTC timestamp
            ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
            String timestamp = nowUtc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String line = timestamp + " - 2FA Code: " + code + System.lineSeparator();

            Path logPath = Paths.get("/cron/last_code.txt");
            // Make sure parent directory exists (on Windows this will be C:\cron)
            Files.createDirectories(logPath.getParent());

            Files.writeString(
                    logPath,
                    line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            System.out.print(line);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
