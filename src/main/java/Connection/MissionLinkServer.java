package Connection;

import Message.MessageUDP;
import Message.Message;
import Message.UpdateMission;
import Message.Package;
import Message.RequestMission;
import Mothership.Mothership;
import Mothership.RoverInfo;
import java.io.IOException;
import java.util.List;
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

    public void processMessageContent(MessageUDP msg, DatagramPacket packet) {
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
    public void sendResponse(DatagramSocket socket, DatagramPacket requestPacket, MessageUDP reply) {
        try {

            List<MessageUDP> fragments = FragManager.fragmentMessage(reply);
            if (fragments.size() > 1) {
                System.out.println("[ML Server] Sending fragmented reply ID " + reply.getMessageId() +
                        " (" + fragments.size() + " parts).");
            } else {
                System.out.println("[ML Server] Sending reply to Rover: ID = " + reply.getMessageId());
            }

            // 2. Enviar todos os fragmentos sequencialmente
            for (MessageUDP frag : fragments) {
                byte[] replyBytes = frag.convertMessageToBytes();

                DatagramPacket responsePacket = new DatagramPacket(
                        replyBytes,
                        replyBytes.length,
                        requestPacket.getAddress(),
                        requestPacket.getPort()
                );

                socket.send(responsePacket);
                if (fragments.size() > 1) {
                    Thread.sleep(2);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public MessageUDP generateReply(MessageUDP msg) {
        // 1. Obter contexto da conexão (Session State)
        // Assumindo que o ID do rover vem na mensagem ou nos dados
        int roverId = -1;
        if (msg.getMessageData() instanceof RequestMission) {
            roverId = ((RequestMission) msg.getMessageData()).getIdRover();
        }
        // Adiciona lógica para extrair ID de outros tipos de mensagem se necessário

        RoverInfo rInfo = (roverId != -1) ? mothership.getRoverById(roverId) : null;
        // 2. CAMADA DE PROTOCOLO: Verificação de Duplicados (Cache Hit)
        if (rInfo != null && msg.getMessageDataType() == MessageUDP.MessageDataTypes.REQUEST_MISSION) {
            // Verifica se o Seq recebido é menor ou igual ao último processado
            if (msg.getSequenceNumber() <= rInfo.getLastProcessedSequenceNumber()) {
                System.out.println("[LinkServer] ⚠️ Retransmissão detetada (Seq " + msg.getSequenceNumber() + ").");

                Message cached = rInfo.getLastSentMessage();
                if (cached != null) {
                    System.out.println("[LinkServer] 🔄 Devolvendo resposta em cache.");
                    return (MessageUDP) cached; // <--- Corta o fluxo aqui e devolve a cópia
                }
            }
        }

        // 3. CAMADA DE APLICAÇÃO: Gerar nova resposta (Chama a Mothership)
        // Se não for duplicado, deixamos a Mothership trabalhar
        MessageUDP reply = mothership.generateReply(msg);

        // 4. CAMADA DE PROTOCOLO: Atualizar Estado e Cache
        if (rInfo != null && reply != null) {
            // Atualiza o último Seq processado
            rInfo.setLastProcessedSequenceNumber(msg.getSequenceNumber());

            // Guarda a resposta na cache se for uma Missão
            if (msg.getMessageDataType() == MessageUDP.MessageDataTypes.REQUEST_MISSION) {
                rInfo.setLastSentMessage(reply);
            }
        }

        return reply;
    }
}
