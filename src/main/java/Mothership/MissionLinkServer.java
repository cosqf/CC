package Mothership;

import java.net.*;
import java.util.Arrays;

public class MissionLinkServer implements Runnable { //UDP
    private int port;

    public MissionLinkServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("MissionLink UDP Server running on port " + port);
            byte[] buffer = new byte[10];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] msg = packet.getData();
                System.out.println("[ML] Received: " + Arrays.toString(msg)); //kinda wacky here but it's gonna change anyway so idrc rn

                // for response
                String ack = "ACK: " + Arrays.toString(msg);
                byte[] ackBytes = ack.getBytes();
                DatagramPacket response = new DatagramPacket(
                        ackBytes, ackBytes.length, packet.getAddress(), packet.getPort());
                socket.send(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
