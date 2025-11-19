package Connection;

import Message.Message;
import Message.Package;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;

public class MissionLinkSender implements Runnable {
    private final DatagramSocket socket;
    private final BlockingQueue<Package> outgoingQueue;

    public MissionLinkSender(DatagramSocket socket, BlockingQueue<Package> outgoingQueue) {
        this.socket = socket;
        this.outgoingQueue = outgoingQueue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Package packageToSend = outgoingQueue.take(); // blocks here

                InetAddress ip = InetAddress.getByName(packageToSend.getToIp());
                int port = packageToSend.getToPort();
                Message message = packageToSend.getMessage();

                byte[] msg = message.convertMessageToBytes();
                DatagramPacket messagePacket = new DatagramPacket(msg, msg.length, ip, port);
                socket.send(messagePacket);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("[ML-S] Failed to send message: " + e.getMessage());
            }
        }
    }
}
