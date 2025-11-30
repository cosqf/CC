package Connection;

import Message.Message;
import Message.RoverInitMessage;
import Message.Package;
import Rover.Rover;
import Utils.UDPPrint;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MissionLinkClient implements Runnable, MissionLinkGeneric {
    private final String serverIP;
    private final int serverPort;
    private final BlockingQueue<Package> outgoingQueue = new LinkedBlockingQueue<>();
    private final Rover rover;
    private MissionLinkReceiver receiver;
    private MissionLinkSender sender;

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
        try {
            DatagramSocket socket = new DatagramSocket();

            this.sender = new MissionLinkSender(socket, this.outgoingQueue);
            this.receiver = new MissionLinkReceiver(socket, this, this.sender);

            Thread senderThread = new Thread(this.sender);
            Thread receiverThread = new Thread(this.receiver);

            senderThread.start();
            receiverThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        this.receiver.stop();
        this.sender.stop();
        System.out.println("[MissionLinkClient] Closing!");
    }

    @Override
    public void processMessageContent(Message msg, DatagramPacket packet) {
        switch (msg.getMessageDataType()) {
            case ROVER_INIT:
                RoverInitMessage message = (RoverInitMessage) msg.getMessageData();
                UDPPrint.logSuccess("RCV", msg, "ID Atribuído: " + message.getId());
                break;

            case MISSION:
                UDPPrint.logSuccess("RCV", msg, "NOVA MISSÃO ACEITE E GUARDADA!");
                break;

            default:
                // Outras mensagens (como ACKs puros) ficam em log normal ou silêncio
                // WiresharkLogger.log("RCV", msg, "ACK/Outro recebido", false);
                break;
        }
        rover.processMessage(msg.getMessageDataType(), msg.getMessageData());
    }

    @Override
    public Message generateReply(Message msg, int ackNum) {
        return this.rover.generateReply(msg, ackNum);
    }
}