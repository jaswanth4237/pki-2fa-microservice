import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RequestSeed {

    public static void main(String[] args) throws Exception {

        String studentId = "24P35A4237"; 
        String repoUrl = "https://github.com/jaswanth4237/pki-2fa-microservice";


        String pubKey = Files.readString(Paths.get("student_public.pem"))
                .replace("\n", "\\n"); 

        String json = """
                {
                  "student_id": "%s",
                  "github_repo_url": "%s",
                  "public_key": "%s"
                }
                """.formatted(studentId, repoUrl, pubKey);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://eajeyq4r3zljoq4rpovy2nthda0vtjqf.lambda-url.ap-south-1.on.aws"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response: " + response.body());

        // Extract encrypted seed and save to file
        if (response.body().contains("encrypted_seed")) {
            String seed = response.body()
                    .split("\"encrypted_seed\":")[1]
                    .split("\"")[1];

            Files.write(Paths.get("encrypted_seed.txt"), seed.getBytes());
            System.out.println("encrypted_seed.txt saved!");
        } else {
            System.out.println("ERROR: No encrypted seed returned");
        }
    }
}
