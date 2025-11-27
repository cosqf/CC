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

    // 1. ALTERAÇÃO: Variável para guardar a referência do Sender
    private MissionLinkSender sender;
    private int lastProcessedSeq = -1; // Variável de memória essencial

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

            this.sender = new MissionLinkSender(socket, this.outgoingQueue);

            Thread senderThread = new Thread(this.sender);
            Thread receiverThread = new Thread(new MissionLinkReceiver(socket, this));

            senderThread.start();
            receiverThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processMessageContent(Message msg, DatagramPacket packet) {
        // 1. Processar ACK (Silencioso, ou podes por log normal se quiseres)
        if (msg.getAckNumber() != -2 && this.sender != null) {
            this.sender.confirmAck(msg.getAckNumber());
        }

        // 2. FILTRO DE DUPLICADOS (VERMELHO)
        if (msg.getSequenceNumber() <= lastProcessedSeq) {
            UDPPrint.logError("RCV", msg, "Já processado. Ignorado.");
            System.out.println(msg);
            return;
        }

        // Atualizar memória
        lastProcessedSeq = msg.getSequenceNumber();

        // 3. PROCESSAMENTO REAL (VERDE)
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
        System.out.println("[ML] Received: " + msg.toString());
        rover.processMessage(msg.getMessageDataType(), msg.getMessageData());
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