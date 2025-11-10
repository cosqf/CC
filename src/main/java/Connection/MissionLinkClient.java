package Connection;

import Message.Message;

import java.io.IOException;
import java.net.*;
import Message.RequestMission;

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
            RequestMission reqM = new RequestMission(1);
            Message message = new Message(1,Message.MessageDataTypes.REQUEST_MISSION, reqM);
            byte[] msg = message.convertMessageToBytes();

            DatagramPacket requestPacket = new DatagramPacket(
                    msg, msg.length, serverAddr, serverPort);
            socket.send(requestPacket);

            byte[] buffer = new byte[msg.length];
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
