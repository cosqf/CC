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

    // 1. ALTERAÇÃO: Variável para guardar a referência do Sender
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
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();

            // 2. ALTERAÇÃO: Instanciar o Sender para a variável da classe antes de iniciar a Thread
            // Isto permite que o Receiver consiga "falar" com ele mais tarde.
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
        System.out.println("[ML] Received: " + msg.toString());

        // 3. ALTERAÇÃO: Lógica de Fiabilidade (Stop-and-Wait)
        // Verifica se a mensagem traz um ACK no cabeçalho (Piggybacking ou ACK puro)
        if (msg.getAckNumber() != -1) {
            System.out.println("[ML-Client] ACK Recebido no cabeçalho: " + msg.getAckNumber());
            if (this.sender != null) {
                // Avisa a thread Sender que o pacote foi confirmado para ela avançar
                this.sender.confirmAck(msg.getAckNumber());
            }
        }

        // Lógica original de processamento de conteúdo
        switch (msg.getMessageDataType()) {
            case ROVER_INIT:
                RoverInitMessage message = (RoverInitMessage) msg.getMessageData();
                rover.setId(message.id);
                break;
            case MISSION:
                System.out.println("[ML] NOVA MISSÃO RECEBIDA!");
                // Aqui deves adicionar a lógica para guardar a missão no Rover
                // Ex: rover.setMission((MissionMessage) msg.getMessageData());
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