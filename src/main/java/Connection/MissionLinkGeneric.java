package Connection;

import Message.MessageUDP;

import java.net.DatagramPacket;

public interface MissionLinkGeneric {
    void processMessageContent(MessageUDP msg, DatagramPacket packet);
    MessageUDP generateReply(MessageUDP msg, int ackNum);
}
