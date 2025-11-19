package Connection;

import Message.Message;
import Message.RoverInitMessage;
import Message.Package;
import Rover.Rover;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MissionLinkClient implements Runnable, MissionLinkGeneric {
    private final String serverIP;
    private final int serverPort;
    private final BlockingQueue<Package> outgoingQueue = new LinkedBlockingQueue<>();
    private final Rover rover;

    public MissionLinkClient(String serverIP, int serverPort, Rover rover) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.rover = rover;
    }

    public void enqueueMessage(Message message) {
        try {
            Package p = new Package(serverIP, serverPort, message);
            outgoingQueue.put(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            Thread senderThread = new Thread(new MissionLinkSender(socket, this.outgoingQueue));
            Thread receiverThread = new Thread(new MissionLinkReceiver(socket, this));

            senderThread.start();
            receiverThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processMessageContent(Message msg, DatagramPacket packet) {
        System.out.println("[ML] Received: " + msg.toString());

        switch (msg.getMessageDataType()) {
            case ROVER_INIT:
                RoverInitMessage message = (RoverInitMessage) msg.getMessageData();
                rover.setId(message.id);
                break;
            case MISSION:
                // get assigned a new mission...
                break;
            default:
                break;
        }
    }

    @Override
    public void sendResponse(DatagramSocket socket, DatagramPacket requestPacket, Message reply) {
        try {
            byte[] replyBytes = reply.convertMessageToBytes();

            DatagramPacket responsePacket = new DatagramPacket(
                    replyBytes, replyBytes.length, requestPacket.getAddress(), requestPacket.getPort());

            socket.send(responsePacket);
            System.out.println("[ML] Sent reply to Mothership: ID = " + reply.getMessageId());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public Message generateReply(Message msg) {
        return this.rover.generateReply(msg);
    }

}