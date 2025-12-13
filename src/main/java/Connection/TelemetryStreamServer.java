package Connection;

import Message.Message;
import Message.RoverTelemetryMessage;
import Mothership.Mothership;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TelemetryStreamServer implements Runnable {
    private int port;
    private Mothership mothership;

    public TelemetryStreamServer(int port, Mothership mothership) {
        this.port = port;
        this.mothership = mothership;
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
        int currentRoverId = -1;

        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
            while (true) {
                int length = in.readInt();
                if (length > 0) {
                    byte[] messageBytes = new byte[length];
                    in.readFully(messageBytes);

                    Message receivedMsg = Message.convertBytesToMessage(messageBytes);
                    mothership.updateRoverInfoWithTelemetry(receivedMsg);

                    if (receivedMsg.getMessageDataType() == Message.MessageDataTypes.ROVER_TELEMETRY) {
                        RoverTelemetryMessage tel = (RoverTelemetryMessage) receivedMsg.getMessageData();
                        currentRoverId = tel.getId();
                    }
                }
            }
        } catch (IOException e) {
            if (currentRoverId != -1) {
                mothership.removeRover(currentRoverId);
            }
        }
    }
}