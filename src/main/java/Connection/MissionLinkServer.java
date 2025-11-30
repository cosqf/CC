package Connection;

import Message.Message;
import Message.UpdateMission;
import Message.Package;
import Message.RequestMission;
import Mothership.Mothership;
import Mothership.RoverInfo;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
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

    public void enqueueMessage(Message message, InetAddress roverIp, int roverPort) {
        try {
            Package p = new Package(roverIp.getHostAddress(), roverPort, message);
            outgoingQueue.put(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(port);
            System.out.println("MissionLink UDP Server running on port " + port);

            MissionLinkSender sender = new MissionLinkSender(socket, this.outgoingQueue);
            Thread senderThread = new Thread(sender);
            Thread receiverThread = new Thread(new MissionLinkReceiver(socket, this, sender));

            receiverThread.start();
            senderThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processMessageContent(Message msg, DatagramPacket packet) {
        System.out.println("[ML] Received: " + msg.toString());

        switch (msg.getMessageDataType()) {
            case ROVER_INIT:
                // the mothership will store the ip/port from the packet
                mothership.storeRoverInfoConnection(msg, packet.getAddress(), packet.getPort());
                break;
            case REQUEST_MISSION:
                mothership.mothershipMissions.createRandomMissionIfEmpty();
                break;
            case MISSION_UPDATE:
                UpdateMission updateMissionMsg = (UpdateMission) msg.getMessageData();
                mothership.mothershipMissions.processMissionUpdate(updateMissionMsg);
            default:
                break;
        }
    }
    @Override
    public Message generateReply(Message msg, int ackNum) {
        return mothership.generateReply(msg, ackNum);
    }
}
