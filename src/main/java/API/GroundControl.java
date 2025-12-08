package API;

import Connection.NetworkConfig;

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

        System.out.println("\nGroundControl connected to: " + baseUrl);
        System.out.println("\n--- WELCOME ---");

        try {
            boolean running = true;
            while (running) {
                System.out.println("1. Check Active Rovers");
                System.out.println("2. Check Active Missions");
                System.out.println("3. Check Past Missions");
                System.out.println("4. Check Future Missions");
                System.out.println("5. Check Last Telemetry Message");
                System.out.println("0. Exit");

                System.out.print("Enter option: ");
                String input = scanner.nextLine();
                int option = -1;
                try {
                    option = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number: ");
                    continue;
                }

                switch (option) {
                    case 1:
                        System.out.println("\nROVER INFO");
                        System.out.println(getResponse("/rovers"));
                        optionMenu("/rovers", scanner);
                        break;
                    case 2:
                        System.out.println("\nACTIVE MISSIONS");
                        System.out.println(getResponse("/missions/active"));
                        optionMenu("/missions/active", scanner);
                        break;
                    case 3:
                        System.out.println("\nPAST MISSIONS");
                        System.out.println(getResponse("/missions/past"));
                        optionMenu("/missions/past", scanner);
                        break;
                    case 4:
                        System.out.println("\nFUTURE MISSIONS");
                        System.out.println(getResponse("/missions/future"));
                        optionMenu("/missions/future", scanner);
                        break;
                    case 5:
                        System.out.println("\nMOST RECENT TELEMETRY DATA");
                        System.out.println(getResponse("/telemetry"));
                        optionMenu("/telemetry", scanner);
                        break;
                    case 0:
                        System.out.println("Exiting Ground Control...");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            scanner.close();
        }
    }

    private void optionMenu(String endpoint, Scanner sc) {
        try {
            boolean running = true;
            while (running) {
                System.out.println("\n1. Refresh");
                System.out.println("0. Back");

                System.out.print("Enter option: ");
                String input = sc.nextLine();
                int option = -1;
                try {
                    option = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number: ");
                    continue;
                }

                switch (option) {
                    case 1:
                        System.out.println(getResponse(endpoint));
                        break;
                    case 0:
                        System.out.println("Going back to the main menu...");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getResponse(String endpoint) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + endpoint))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "Error: received status code " + response.statusCode();
            }
            return response.body();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        NetworkConfig networkConfig = new NetworkConfig();
        String ms_ip = networkConfig.getIp(NetworkConfig.ID.MOTHERSHIP_IP);
        String api_port = networkConfig.getIp(NetworkConfig.ID.API_SERVER);

        GroundControl gc = new GroundControl(ms_ip, Integer.parseInt(api_port));
        gc.start();
    }
}