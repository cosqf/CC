package Mothership;

import Message.*;
import Utils.Point3D;
import Mission.Mission;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Mothership { // controller
    // Mapa para guardar os Rovers (evita IndexOutOfBounds)
    private Map<Integer, RoverInfo> rovers = new HashMap<>();
    private int localSequenceNumber = 0;

    public void updateRoverInfoWithTelemetry(Message msg) {
        if (msg.getMessageDataType() != Message.MessageDataTypes.ROVER_TELEMETRY) return;

        RoverTelemetryMessage telemetry = (RoverTelemetryMessage) msg.getMessageData();
        int roverId = telemetry.getId();
        RoverInfo roverInfo = this.rovers.get(roverId);

        roverInfo.updateLastTelemetryMessage(telemetry);
        roverInfo.updateLastActiveTimestamp(System.currentTimeMillis());
        System.out.println("[Mothership] Telemetria atualizada para Rover " + roverId);
    }

    public void storeRoverInfoConnection (Message msg, InetAddress ip, int port) {
        if (msg.getMessageDataType() != Message.MessageDataTypes.ROVER_INIT) return;
        RoverInitMessage roverInit = (RoverInitMessage) msg.getMessageData();
        int roverId = roverInit.getId();
        RoverInfo roverInfo = this.rovers.get(roverId);
        if (roverInfo != null) {
            System.out.println("[Mothership] Rover with that id already exists!");
            return;
        }
        System.out.println("[Mothership] New rover initiating: ID " + roverId);
        roverInfo = new RoverInfo(roverId, null, 0);
        this.rovers.put(roverId, roverInfo);

        roverInfo.setRoverConnection (ip, port);
        roverInfo.updateLastActiveTimestamp(System.currentTimeMillis());
        System.out.println("Stored the ip and port of Rover " + roverId);
    }

    public static void main(String[] args) {
        Mothership mothership = new Mothership();
        MothershipConnection connection = new MothershipConnection(mothership);
        connection.startServer();
    }

    public Message generateReply(Message receivedMsg) {
        Message reply = null;

        // 1. CALCULAR O ACK (Lógica TCP)
        int payloadSize = 0;
        if (receivedMsg.getMessageData() != null) {
            payloadSize = receivedMsg.getMessageData().convertMessageDataToBytes().length;
        }
        int ackNum = receivedMsg.getSequenceNumber() + (payloadSize > 0 ? payloadSize : 1);

        switch (receivedMsg.getMessageDataType()) {

            // --- PREVENÇÃO DE LOOP ---
            case ACK:
                return null; // Não responder a ACKs para evitar loops infinitos

            // --- INICIALIZAÇÃO ---
            case ROVER_INIT:
                RoverInitMessage initMsg = (RoverInitMessage) receivedMsg.getMessageData();
                int idParaRegistar;

                // Decidir ID (Manter o existente ou criar novo)
                if (initMsg.getId() > 0) {
                    idParaRegistar = initMsg.getId();
                } else {
                    idParaRegistar = rovers.size() + 1;
                }

                reply = new Message(
                        this.localSequenceNumber++,
                        ackNum,
                        Message.MessageDataTypes.ROVER_INIT,
                        new RoverInitMessage(idParaRegistar)
                );
                break;

            // --- PEDIDO DE MISSÃO (Com Cache) ---
            case REQUEST_MISSION:
                RequestMission req = (RequestMission) receivedMsg.getMessageData();
                RoverInfo rInfo = this.rovers.get(req.getIdRover());

                // A. VERIFICAR SE É RETRANSMISSÃO
                // Se conhecemos o Rover E o número de sequência é antigo...
                if (rInfo != null && receivedMsg.getSequenceNumber() <= rInfo.getLastProcessedSequenceNumber()) {
                    System.out.println("[Mothership] ⚠️ Detetada retransmissão (Seq " + receivedMsg.getSequenceNumber() + ").");

                    // Tentar recuperar a resposta da CACHE
                    Message cached = rInfo.getLastSentMessage();
                    if (cached != null) {
                        System.out.println("[Mothership] 🔄 Reenviando a Missão guardada em cache.");
                        return cached; // <--- RETORNA A MENSAGEM ORIGINAL (MISSÃO)
                    }
                    // Se não houver cache, sai do switch e manda ACK simples
                    break;
                }

                // B. PROCESSAMENTO DE PEDIDO NOVO
                if (rInfo != null) {
                    rInfo.setLastProcessedSequenceNumber(receivedMsg.getSequenceNumber());
                }

                System.out.println("[Mothership] A criar Nova Missão para Rover " + req.getIdRover());
                Mission novaMissao = new Mission(
                        req.getIdRover(), // Usa o ID correto do Rover
                        Mission.MissionType.EXPLORE,
                        new Point3D(50,50,0),
                        100, 600, 60
                );

                reply = new Message(
                        this.localSequenceNumber++,
                        ackNum,
                        Message.MessageDataTypes.MISSION,
                        new MissionMessage(novaMissao)
                );

                // C. GUARDAR NA CACHE (Para futuras retransmissões)
                if (rInfo != null) {
                    rInfo.setLastSentMessage(reply);
                }
                break;

            default:
                break;
        }

        // Fallback: Envia ACK puro (para ACKs perdidos sem dados ou erros)
        if (reply == null) {
            reply = new Message(
                    this.localSequenceNumber++,
                    ackNum,
                    Message.MessageDataTypes.ACK,
                    new ACKMessage(receivedMsg.getSequenceNumber())
            );
        }

        return reply;
    }
    public void removeRover(int roverId) {
        if (rovers.containsKey(roverId)) {
            rovers.remove(roverId);
            System.out.println("[Mothership] Rover " + roverId + " desconectado e removido da lista.");
        }
    }

    public Collection<RoverInfo> getRoverInfo() {
        return this.rovers.values();
    }

//    public Collection<RoverInfo> getActiveMissions() {
//        return mothershipMissions.getActiveMissions();
//    }
//
//    public Collection<RoverInfo> getPastMissions() {
//        return mothershipMissions.getActiveMissions();
//    }

    public ArrayList<RoverTelemetryMessage> getLastTelemetry() {
        ArrayList<RoverTelemetryMessage> res = new ArrayList<RoverTelemetryMessage>();
        for (RoverInfo i : this.rovers.values()) {
            res.add(i.getLastTelemetryMessage());
        }
        return res;
    }
}