package Connection;

import Message.Message;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public interface MissionLinkGeneric {
    void processMessageContent(Message msg, DatagramPacket packet);
    Message generateReply(Message msg, int ackNum);
}
