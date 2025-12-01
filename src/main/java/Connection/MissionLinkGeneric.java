package Connection;

import Message.MessageUDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public interface MissionLinkGeneric {
    void processMessageContent(MessageUDP msg, DatagramPacket packet);
    void sendResponse(DatagramSocket socket, DatagramPacket packet, MessageUDP reply);
    MessageUDP generateReply(MessageUDP msg);
}
