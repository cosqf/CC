    package Mothership;

    import Message.*;

    import Mission.Mission;
    import java.net.InetAddress;
    import java.util.ArrayList;
    import java.util.Collection;
    import Message.*;
    import Message.MissionMessage;
    import java.util.HashMap;
    import java.util.Map;

    public class Mothership { // controller
        private final Map<Integer, RoverInfo> rovers = new HashMap<>();
        public MothershipMissions mothershipMissions;


    public static void main(String[] args) {
        Mothership mothership = new Mothership();
        MothershipConnection connection = new MothershipConnection(mothership);
        connection.startServer();
        mothership.mothershipMissions = new MothershipMissions();
    }

    // --- LÓGICA DE TELEMETRIA ---
    public void updateRoverInfoWithTelemetry(Message msg) {
        if (msg.getMessageDataType() != Message.MessageDataTypes.ROVER_TELEMETRY) return;

            RoverTelemetryMessage telemetry = (RoverTelemetryMessage) msg.getMessageData();
            int roverId = telemetry.getId();
            RoverInfo roverInfo = this.rovers.get(roverId);

        // Proteção contra NullPointerException (Race Condition)
        if (roverInfo == null) {
            System.out.println("[Mothership] ⚠️ Recebida telemetria de Rover desconhecido (ID " + roverId + "). Ignorando.");
            return;
        }

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
            roverInfo = new RoverInfo(roverId, ip, port);
            this.rovers.put(roverId, roverInfo);

            roverInfo.updateLastActiveTimestamp(System.currentTimeMillis());
        }

    // --- LÓGICA DE RESPOSTA ---
    public MessageUDP generateReply(MessageUDP receivedMsg) {
        MessageUDP reply = null;

        // 1. CALCULAR O ACK (Lógica TCP)
        int payloadSize = 0;
        if (receivedMsg.getMessageData() != null) {
            payloadSize = receivedMsg.getMessageData().convertMessageDataToBytes().length;
        }
        int ackNum = receivedMsg.getSequenceNumber() + (payloadSize > 0 ? payloadSize : 1);

        // Campos de fragmentação padrão para pacotes únicos
        int fragID = 0, fragIdx = 0, totalFrags = 1;

        switch (receivedMsg.getMessageDataType()) {

            case ACK:
                return null; // ACKs não precisam de resposta

                // --- INICIALIZAÇÃO ---
                case ROVER_INIT:
                    RoverInitMessage initMsg = (RoverInitMessage) receivedMsg.getMessageData();
                    int idParaRegistar;

                    int givenID = initMsg.getId();
                    RoverInfo rover = this.rovers.get(givenID);
                    if (rover.getLastTelemetryMessage() != null) idParaRegistar = -1;
                    else idParaRegistar = givenID;

                // Obter RoverInfo para usar o contador DELE
                RoverInfo rInfoInit = this.rovers.get(idParaRegistar);
                int seqInit = (rInfoInit != null) ? rInfoInit.getAndIncrementOutputSequenceNumber() : 0;

                reply = new MessageUDP(
                        seqInit, // Sequência específica deste Rover
                        ackNum,
                        fragID, fragIdx, totalFrags,
                        Message.MessageDataTypes.ROVER_INIT,
                        new RoverInitMessage(idParaRegistar)
                );
                break;

            // --- PEDIDO DE MISSÃO ---
            case REQUEST_MISSION:
                RequestMission req = (RequestMission) receivedMsg.getMessageData();

                System.out.println("[Mothership] A criar Nova Missão para Rover " + req.getIdRover());
                Mission mission = this.mothershipMissions.getMission();

                if (mission == null) {
                    break;
                }
                mission.setRoverId(req.getIdRover());
                mothershipMissions.startMission(mission);

                // Usar contador de sequência específico deste Rover
                RoverInfo rInfoMission = this.rovers.get(req.getIdRover());
                int seqMission = (rInfoMission != null) ? rInfoMission.getAndIncrementOutputSequenceNumber() : 0;

                reply = new MessageUDP(
                        seqMission, // Sequência específica
                        ackNum,
                        fragID, fragIdx, totalFrags,
                        Message.MessageDataTypes.MISSION,
                        new MissionMessage(mission));
                break;

                default:
                    break;
            }

        // Fallback: Envia ACK puro
        if (reply == null) {
            reply = new MessageUDP(
                    0, // ACK puro pode ir com 0 ou contador global se não for crítico
                    ackNum,
                    0, 0, 1,
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

    public Collection<Mission> getActiveMissions() {
        return mothershipMissions.getActiveMissions();
    }

    public Collection<Mission> getPastMissions() {
        return mothershipMissions.getPastMissions();
    }

    public Collection<Mission> getFutureMissions() {
        return mothershipMissions.getFutureMissions();
    }


    public ArrayList<RoverTelemetryMessage> getLastTelemetry() {
        ArrayList<RoverTelemetryMessage> res = new ArrayList<RoverTelemetryMessage>();
        for (RoverInfo i : this.rovers.values()) {
            res.add(i.getLastTelemetryMessage());
        }
        return res;
    }

    // retorna info de um rover especifico, por ID
    public RoverInfo getRoverById(int id) {
        return this.rovers.get(id);
    }
}