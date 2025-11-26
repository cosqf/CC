package Connection;

import Message.Message;
import Message.UpdateMission;
import Message.Package;
import Message.RequestMission;
import Mothership.Mothership;
import Mothership.RoverInfo;
import java.io.IOException;
import java.net.*;
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

    @Override
    public void run() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(port);
            System.out.println("MissionLink UDP Server running on port " + port);

            Thread receiverThread = new Thread(new MissionLinkReceiver(socket, this));
            Thread senderThread = new Thread(new MissionLinkSender(socket, this.outgoingQueue));

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
    public void sendResponse(DatagramSocket socket, DatagramPacket requestPacket, Message reply) {
        try {
            byte[] replyBytes = reply.convertMessageToBytes();

            DatagramPacket responsePacket = new DatagramPacket(
                    replyBytes, replyBytes.length, requestPacket.getAddress(), requestPacket.getPort());

            socket.send(responsePacket);
            System.out.println("[ML] Sent reply to Rover: ID = " + reply.getMessageId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Message generateReply(Message msg) {
        // 1. Obter contexto da conexão (Session State)
        // Assumindo que o ID do rover vem na mensagem ou nos dados
        int roverId = -1;
        if (msg.getMessageData() instanceof RequestMission) {
            roverId = ((RequestMission) msg.getMessageData()).getIdRover();
        }
        // Adiciona lógica para extrair ID de outros tipos de mensagem se necessário

        RoverInfo rInfo = (roverId != -1) ? mothership.getRoverById(roverId) : null;
        // 2. CAMADA DE PROTOCOLO: Verificação de Duplicados (Cache Hit)
        if (rInfo != null && msg.getMessageDataType() == Message.MessageDataTypes.REQUEST_MISSION) {
            // Verifica se o Seq recebido é menor ou igual ao último processado
            if (msg.getSequenceNumber() <= rInfo.getLastProcessedSequenceNumber()) {
                System.out.println("[LinkServer] ⚠️ Retransmissão detetada (Seq " + msg.getSequenceNumber() + ").");

                Message cached = rInfo.getLastSentMessage();
                if (cached != null) {
                    System.out.println("[LinkServer] 🔄 Devolvendo resposta em cache.");
                    return cached; // <--- Corta o fluxo aqui e devolve a cópia
                }
            }
        }

        // 3. CAMADA DE APLICAÇÃO: Gerar nova resposta (Chama a Mothership)
        // Se não for duplicado, deixamos a Mothership trabalhar
        Message reply = mothership.generateReply(msg);

        // 4. CAMADA DE PROTOCOLO: Atualizar Estado e Cache
        if (rInfo != null && reply != null) {
            // Atualiza o último Seq processado
            rInfo.setLastProcessedSequenceNumber(msg.getSequenceNumber());

            // Guarda a resposta na cache se for uma Missão
            if (msg.getMessageDataType() == Message.MessageDataTypes.REQUEST_MISSION) {
                rInfo.setLastSentMessage(reply);
            }
        }

        return reply;
    }
}
