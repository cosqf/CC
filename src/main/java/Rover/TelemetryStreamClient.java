package Rover;

import java.io.*;
import java.net.*;

public class TelemetryStreamClient implements Runnable {
    private final String serverIP;
    private final int serverPort;

    public TelemetryStreamClient(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(serverIP, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            // ^ i think PrintWriter only works for strings
            System.out.println("[TS] Connected to Mothership for telemetry.");

            while (true) {
                String telemetry = "This is supposed to a telemetry message";
                out.println(telemetry);
                System.out.println("[TS] Sent telemetry: " + telemetry);

                Thread.sleep(120000); // every 2 minutes
            }

        } catch (IOException e) {
            System.out.println("[TS] Connection closed or lost.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
