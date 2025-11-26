package API;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class GroundControl {

    private final String baseUrl;
    private final HttpClient client;

    public GroundControl(String ip, int port) {
        this.baseUrl = "http://" + ip + ":" + port;
        this.client = HttpClient.newHttpClient();
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("GroundControl connected to: " + baseUrl);

        try {
            boolean running = true;
            while (running) {
                System.out.println("\n--- WELCOME ---");
                System.out.println("1. Check Active Rovers");
                System.out.println("2. Check Active Missions");
                System.out.println("3. Check Past Missions");
                System.out.println("4. Check Last Telemetry Message");
                System.out.println("5. Exit");

                System.out.print("Enter option: ");
                int option = scanner.nextInt();
                scanner.nextLine(); // to consume leftover newline

                switch (option) {
                    case 1:
                        System.out.println("\nROVER INFO");
                        System.out.println(getResponse("/rovers"));
                        break;
//                    case 2:
//                        System.out.println("\nACTIVE MISSIONS");
//                        System.out.println(getResponse("/missions/active");
//                        break;
//                    case 3:
//                        System.out.println("\nPAST MISSIONS");
//                        System.out.println(getResponse("/missions/past");
//                        break;
                    case 4:
                        System.out.println("\nMOST RECENT TELEMETRY DATA");
                        System.out.println(getResponse("/telemetry"));
                        break;
                    case 5:
                        System.out.println("Exiting Ground Control...");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private String getResponse(String endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Error: received status code " + response.statusCode());
            return null;
        }

        return response.body();
    }

    public static void main(String[] args) {
        GroundControl gc = new GroundControl("10.0.0.1", 7000);
        gc.start();
    }
}