package Connection;

import Message.Message;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TelemetryStreamClient implements Runnable {
    private final String serverIP;
    private final int serverPort;
    private final BlockingQueue<Message> outgoingQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public TelemetryStreamClient(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void enqueueMessage(Message message) throws TelemetryStreamNotRunning {
        if (!running) throw new TelemetryStreamNotRunning();
        try {
            outgoingQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(serverIP, serverPort);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            System.out.println("[TS] Connected to Mothership for telemetry.");

            while (running) {
                try {
                    Message message = outgoingQueue.take();
                    byte[] msgBytes = message.convertMessageToBytes();

                    out.writeInt(msgBytes.length);
                    out.write(msgBytes);
                    out.flush();

                    //System.out.println("[TS] Sent telemetry");
                } catch (InterruptedException e) {
                    System.out.println("[TS] Connection thread interrupted.");
                    Thread.currentThread().interrupt();
                    running = false;
                } catch (IOException e) {
                    if (running) {
                        System.out.println("[TS] IO Error: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("[TS] Connection closed or lost.");
            running = false;
        }
    }

    public class TelemetryStreamNotRunning extends Exception {
        public TelemetryStreamNotRunning (){ super ();}
    }

}
