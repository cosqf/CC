package Connection;

import Message.Message;
import Message.Package;
import Mothership.Mothership;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MissionLinkServer implements Runnable, MissionLinkGeneric { //UDP
    private int port;
    private Mothership mothership;
    private final BlockingQueue<Package> outgoingQueue = new LinkedBlockingQueue<>();

    public MissionLinkServer(int port, Mothership mothership) {
        this.port = port;
        this.mothership = mothership;
    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(port);
            System.out.println("MissionLink UDP Server running on port " + port);

            Thread receiverThread = new Thread(new MissionLinkReceiver(socket, this));
            Thread senderThread = new Thread(new MissionLinkSender(socket, this.outgoingQueue));

            receiverThread.start();
            senderThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processMessageContent(Message msg, DatagramPacket packet) {
        System.out.println("[ML] Received: " + msg.toString());

    }

    @Override
    public void sendResponse(DatagramSocket socket, DatagramPacket requestPacket, Message reply) {
        try {
            byte[] replyBytes = reply.convertMessageToBytes();

            DatagramPacket responsePacket = new DatagramPacket(
                    replyBytes, replyBytes.length, requestPacket.getAddress(), requestPacket.getPort());

            socket.send(responsePacket);
            System.out.println("[ML] Sent reply to Rover: Message ID = " + reply.getMessageId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Message generateReply(Message msg) {
        return mothership.generateReply(msg);
    }
}
