package Connection;

import Message.Message;

import Message.MessageUDP;
import Utils.UDPPrint;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

import java.util.concurrent.ConcurrentHashMap;

public class MissionLinkReceiver implements Runnable {
    private final DatagramSocket socket;
    private final MissionLinkGeneric ML;
    private boolean running = true;
    private final MissionLinkSender sender;
    // Buffer de Remontagem
    private final Map<Integer, List<MessageUDP>> reassemblyBuffer = new ConcurrentHashMap<>();

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
                socket.receive(packet);
                String address = packet.getAddress().getHostAddress() + ":" +  packet.getPort();

                byte[] msgBytes = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, msgBytes, 0, packet.getLength());

                MessageUDP receivedMsg = MessageUDP.convertBytesToMessageUDP(msgBytes);
                //System.out.println("[ML RECEIVER] MESSAGE: " + receivedMsg.toString());

                if (receivedMsg.getAckNumber() != -2) {
                    //System.out.println("CONFIRMED ACK: " + receivedMsg.getAckNumber());
                    this.sender.confirmAck(receivedMsg.getAckNumber(), address);
                }

                MessageUDP finalMessage = null;
                if (!receivedMsg.isFragmented()) {
                    finalMessage = receivedMsg;
                } else {
                    int fragID = receivedMsg.getFragmentID();

                    reassemblyBuffer.putIfAbsent(fragID, new ArrayList<>());
                    List<MessageUDP> parts = reassemblyBuffer.get(fragID);

                    boolean alreadyExists = false;
                    for (MessageUDP existing : parts) {
                        if (existing.getFragmentIndex() == receivedMsg.getFragmentIndex()) {
                            alreadyExists = true;
                            UDPPrint.logError("RCV", existing, "Já processado. Ignorado.");
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        parts.add(receivedMsg);
                        // Só verificamos se está completo se adicionámos uma peça nova
                        if (parts.size() == receivedMsg.getTotalFragments()) {
                            // System.out.println("[Receiver] Reassembling ID " + fragID + "...");
                            finalMessage = FragManager.reassembleMessage(parts);
                            reassemblyBuffer.remove(fragID);
                        }
                    }
                }
                if (finalMessage != null) {
                    ML.processMessageContent(finalMessage, packet);

                    // calculate ACK
                    int payloadSize = 0;
                    if (finalMessage.getMessageData() != null) {
                        payloadSize = finalMessage.getMessageData().convertMessageDataToBytes().length;
                    }
                    int ackNum = finalMessage.getSequenceNumber() + (payloadSize > 0 ? payloadSize : 1);

                    MessageUDP reply = ML.generateReply(finalMessage, ackNum);
                    if (reply != null) {
                        if (reply.getMessageDataType() != Message.MessageDataTypes.ACK) {
                            sender.setLastSentReply(address, reply);
                        }
                        sender.sendMessage(reply, packet.getAddress().getHostAddress(), packet.getPort());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Closing ML receiver!");
    }
}