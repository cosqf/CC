package Connection;

import Message.MessageUDP;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

public class MissionLinkReceiver implements Runnable {
    private final DatagramSocket socket;
    private final MissionLinkGeneric ML;
    private boolean running = true;

    // Buffer de Remontagem
    private final Map<Integer, List<MessageUDP>> reassemblyBuffer = new HashMap<>();

    public MissionLinkReceiver(DatagramSocket socket,  MissionLinkGeneric ML) {
        this.socket = socket;
        this.ML = ML;
    }
    public void stop() {running = false;}

    @Override
    public void run() {
        byte[] buffer = new byte[1500];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] msgBytes = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, msgBytes, 0, packet.getLength());

                MessageUDP receivedMsg = MessageUDP.convertBytesToMessageUDP(msgBytes);

                MessageUDP finalMessage = null;

                if (!receivedMsg.isFragmented()) {
                    finalMessage = receivedMsg;
                } else {
                    int fragID = receivedMsg.getFragmentID();

                    reassemblyBuffer.putIfAbsent(fragID, new ArrayList<>());
                    List<MessageUDP> parts = reassemblyBuffer.get(fragID);

                    // --- CORREÇÃO CRÍTICA: VERIFICAR DUPLICADOS ---
                    boolean alreadyExists = false;
                    for (MessageUDP existing : parts) {
                        if (existing.getFragmentIndex() == receivedMsg.getFragmentIndex()) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        parts.add(receivedMsg);

                        // Debug opcional
                        System.out.println("[Receiver] Frag " + (receivedMsg.getFragmentIndex()+1) +
                                "/" + receivedMsg.getTotalFragments());

                        // Só verificamos se está completo se adicionámos uma peça nova
                        if (parts.size() == receivedMsg.getTotalFragments()) {
                            // System.out.println("[Receiver] Reassembling ID " + fragID + "...");
                            finalMessage = FragManager.reassembleMessage(parts);
                            reassemblyBuffer.remove(fragID);
                        }
                    } else {
                        // System.out.println("[Receiver] Ignored duplicate fragment " + (receivedMsg.getFragmentIndex()+1));
                    }
                    // ----------------------------------------------
                }

                if (finalMessage != null) {
                    ML.processMessageContent(finalMessage, packet);

                    MessageUDP reply = ML.generateReply(finalMessage);
                    if (reply != null) ML.sendResponse(socket, packet, reply);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Closing ML receiver!");
    }
}