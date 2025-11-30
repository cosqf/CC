package Connection;

import Message.Message;
import Utils.UDPPrint;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class MissionLinkReceiver implements Runnable {
    private final DatagramSocket socket;
    private final MissionLinkGeneric ML;
    private boolean running = true;
    private final ConcurrentHashMap <String, Integer> lastProcessedSeq = new ConcurrentHashMap<>();
    private final MissionLinkSender sender;

    public MissionLinkReceiver(DatagramSocket socket,  MissionLinkGeneric ML, MissionLinkSender sender) {
        this.socket = socket;
        this.ML = ML;
        this.sender = sender;
    }
    public void stop() {running = false;}

    @Override
    public void run() {
        byte[] buffer = new byte[1500];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // blocks here until a new packet is received

                String address = packet.getAddress().getHostAddress();
                byte[] msg = packet.getData();
                Message receivedMsg = Message.convertBytesToMessage(msg);
                System.out.println("[ML RECEIVER] MESSAGE: " + receivedMsg.toString());

                boolean isDuplicate = checkACKandDUP(receivedMsg, address);
                if (isDuplicate) {
                    UDPPrint.logError("RCV", receivedMsg, "Já processado. Reenviando ACK.");
                    Message lastSentReply = sender.getLastSentReply(address);
                    if (lastSentReply != null) {
                        sender.sendMessage(lastSentReply, address, packet.getPort());
                    }
                    continue;
                }
                ML.processMessageContent(receivedMsg, packet);


                // calculate ACK
                int payloadSize = 0;
                if (receivedMsg.getMessageData() != null) {
                    payloadSize = receivedMsg.getMessageData().convertMessageDataToBytes().length;
                }
                int ackNum = receivedMsg.getSequenceNumber() + (payloadSize > 0 ? payloadSize : 1);


                Message reply = ML.generateReply(receivedMsg, ackNum);

                if (reply != null) {
                    if (reply.getMessageDataType() != Message.MessageDataTypes.ACK) {
                        sender.setLastSentReply(address, reply);
                    }
                    sender.sendMessage(reply, address, packet.getPort());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Closing ML receiver!");
    }

    public boolean checkACKandDUP (Message msg, String address){
        // process ACK
        if (msg.getAckNumber() != -2 && this.sender != null) {
            System.out.println("SENDING CONFIRM ACK: " + msg.getAckNumber());
            this.sender.confirmAck(msg.getAckNumber(), address);
        }
        // duplicate filter
        if (msg.getSequenceNumber() <= lastProcessedSeq.get(address)) return true;

        // update memory
        lastProcessedSeq.put(address, msg.getSequenceNumber());
        System.out.println("LAST PROCESSED SEQ: " + lastProcessedSeq);
        return false;
    }
}