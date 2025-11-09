package Rover;

import java.io.IOException;
import java.net.*;

public class MissionLinkClient implements Runnable {
    private final String serverIP;
    private final int serverPort;

    public MissionLinkClient(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddr = InetAddress.getByName(serverIP);

            // just for test
            String request = "REQUEST_MISSION:Rover1";
            DatagramPacket requestPacket = new DatagramPacket(
                    request.getBytes(), request.length(), serverAddr, serverPort);
            socket.send(requestPacket);

            byte[] buffer = new byte[10];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            System.out.println("[ML] Received from Mothership: " + response);

        } catch (IOException e) {
            System.out.println("[ML] Connection closed or lost.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
