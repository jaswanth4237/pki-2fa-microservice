import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;

public class Server extends NanoHTTPD {

    private final Gson gson = new Gson();

    public Server() throws IOException {
        super(8080);

        // Start NanoHTTPD in persistent mode
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("Server running on http://localhost:8080");

        // Keep JVM alive so Docker container does not exit
        Thread keepAlive = new Thread(() -> {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException ignored) {}
        });
        keepAlive.setDaemon(false);
        keepAlive.start();
    }

    public static void main(String[] args) {
        try {
            new Server();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            if (uri.equals("/decrypt-seed") && method == Method.POST) {
                return handleDecryptSeed(session);
            }

            if (uri.equals("/generate-2fa") && method == Method.GET) {
                return handleGenerate2FA();
            }

            if (uri.equals("/verify-2fa") && method == Method.POST) {
                return handleVerify2FA(session);
            }

            return json(404, "{\"error\": \"Not found\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return json(500, "{\"error\": \"Server error\"}");
        }
    }

    // POST /decrypt-seed
    private Response handleDecryptSeed(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String bodyJson = files.get("postData");
            Map body = gson.fromJson(bodyJson, Map.class);

            if (body == null || !body.containsKey("encrypted_seed")) {
                return json(400, "{\"error\": \"Missing encrypted_seed\"}");
            }

            String encrypted = body.get("encrypted_seed").toString();
            String hexSeed = DecryptSeedUtil.decrypt(encrypted);

            if (hexSeed == null) {
                return json(500, "{\"error\": \"Decryption failed\"}");
            }

            // ensure directory exists and save seed
            Path dataDir = Paths.get("/data");
            if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
            Files.writeString(Paths.get("/data/seed.txt"), hexSeed);

            return json(200, "{\"status\":\"ok\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return json(500, "{\"error\": \"Decryption failed\"}");
        }
    }

    // GET /generate-2fa
    private Response handleGenerate2FA() {
        try {
            Path seedPath = Paths.get("/data/seed.txt");
            if (!Files.exists(seedPath)) {
                return json(500, "{\"error\": \"Seed not decrypted yet\"}");
            }

            String hexSeed = Files.readString(seedPath).trim();
            Map<String, Object> result = GenerateTOTPUtil.generate(hexSeed);
            return json(200, gson.toJson(result));
        } catch (Exception e) {
            e.printStackTrace();
            return json(500, "{\"error\": \"Failed to generate code\"}");
        }
    }

    // POST /verify-2fa
    private Response handleVerify2FA(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String bodyJson = files.get("postData");
            Map body = gson.fromJson(bodyJson, Map.class);

            if (body == null || !body.containsKey("code")) {
                return json(400, "{\"error\": \"Missing code\"}");
            }

            String code = body.get("code").toString();
            Path seedPath = Paths.get("/data/seed.txt");
            if (!Files.exists(seedPath)) {
                return json(500, "{\"error\": \"Seed not decrypted yet\"}");
            }

            String hexSeed = Files.readString(seedPath).trim();
            boolean valid = VerifyTOTPUtil.verify(hexSeed, code);
            return json(200, "{\"valid\": " + valid + "}");
        } catch (Exception e) {
            e.printStackTrace();
            return json(500, "{\"error\": \"Verification failed\"}");
        }
    }

    // Helper - JSON Response
    private Response json(int statusCode, String json) {
        return newFixedLengthResponse(Response.Status.lookup(statusCode), "application/json", json);
    }
}
