package Mothership;

import java.io.*;
import java.net.*;

public class TelemetryStreamServer implements Runnable {
    private int port;

    public TelemetryStreamServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Telemetry TCP Server running on port " + port);

            while (true) {
                Socket roverSocket = serverSocket.accept();
                new Thread(() -> handleRover(roverSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRover(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[TS] Telemetry: " + line); // not printing
            }
        } catch (IOException e) {
            System.out.println("[TS] Rover disconnected.");
        }
    }
}

