import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;

public class Server extends NanoHTTPD {

    Gson gson = new Gson();

    public Server() throws IOException {
        super(8080);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("Server running on http://localhost:8080");
    }

    public static void main(String[] args) {
        try {
            new Server();
        } catch (Exception e) {
            e.printStackTrace();
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

    // ----------------------------------------------------
    // 1. POST /decrypt-seed
    // ----------------------------------------------------
    private Response handleDecryptSeed(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);

            String bodyJson = files.get("postData");
            Map body = gson.fromJson(bodyJson, Map.class);

            if (!body.containsKey("encrypted_seed")) {
                return json(400, "{\"error\": \"Missing encrypted_seed\"}");
            }

            String encrypted = body.get("encrypted_seed").toString();

            String hexSeed = DecryptSeedUtil.decrypt(encrypted);

            if (hexSeed == null) {
                return json(500, "{\"error\": \"Decryption failed\"}");
            }

            // Save seed.txt in current folder
           Files.write(Paths.get("/data/seed.txt"), hexSeed.getBytes());


            return json(200, "{\"status\":\"ok\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return json(500, "{\"error\": \"Decryption failed\"}");
        }
    }

    // ----------------------------------------------------
    // 2. GET /generate-2fa
    // ----------------------------------------------------
    private Response handleGenerate2FA() {
        try {
           if (!Files.exists(Paths.get("/data/seed.txt"))) {
    return json(500, "{\"error\": \"Seed not decrypted yet\"}");
}

String hexSeed = Files.readString(Paths.get("/data/seed.txt")).trim();

            Map<String, Object> result = GenerateTOTPUtil.generate(hexSeed);

            return json(200, gson.toJson(result));

        } catch (Exception e) {
            e.printStackTrace();
            return json(500, "{\"error\": \"Failed to generate code\"}");
        }
    }

    // ----------------------------------------------------
    // 3. POST /verify-2fa
    // ----------------------------------------------------
    private Response handleVerify2FA(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);

            String bodyJson = files.get("postData");
            Map body = gson.fromJson(bodyJson, Map.class);

            if (!body.containsKey("code")) {
                return json(400, "{\"error\": \"Missing code\"}");
            }

            String code = body.get("code").toString();

            if (!Files.exists(Paths.get("/data/seed.txt"))) {
    return json(500, "{\"error\": \"Seed not decrypted yet\"}");
}

String hexSeed = Files.readString(Paths.get("/data/seed.txt")).trim();

            boolean valid = VerifyTOTPUtil.verify(hexSeed, code);

            return json(200, "{\"valid\": " + valid + "}");

        } catch (Exception e) {
            e.printStackTrace();
            return json(500, "{\"error\": \"Verification failed\"}");
        }
    }

    // ----------------------------------------------------
    // Helper - JSON Response
    // ----------------------------------------------------
    private Response json(int statusCode, String json) {
        return newFixedLengthResponse(
                Response.Status.lookup(statusCode),
                "application/json",
                json
        );
    }
}
