    package Mothership;

    import Message.*;

    import Mission.Mission;
    import java.net.InetAddress;
    import java.util.*;

    import Message.*;
    import Message.MissionMessage;

    public class Mothership { // controller
        private final Map<Integer, RoverInfo> rovers = new HashMap<>();
        public MothershipMissions mothershipMissions;
        public MothershipConnection connection;

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
            roverInfo = new RoverInfo(roverId, ip, port);
            this.rovers.put(roverId, roverInfo);

            roverInfo.updateLastActiveTimestamp(System.currentTimeMillis());
        }
        public void sendMission(MissionMessage msg) {
            connection.sendMission(msg);
        }

        public static void main(String[] args) {
            Mothership mothership = new Mothership();
            mothership.connection = new MothershipConnection(mothership);
            mothership.connection.startServer();
            mothership.mothershipMissions = new MothershipMissions(mothership);
        }

        public Message generateReply(Message receivedMsg, int ackNum) {
            Message reply = null;

            switch (receivedMsg.getMessageDataType()) {
                // --- PREVENÇÃO DE LOOP ---
                case ACK:
                    return null; // Não responder a ACKs para evitar loops infinitos

                // --- INICIALIZAÇÃO ---
                case ROVER_INIT:
                    RoverInitMessage initMsg = (RoverInitMessage) receivedMsg.getMessageData();
                    int idParaRegistar;

                    int givenID = initMsg.getId();
                    RoverInfo rover = this.rovers.get(givenID);
                    if (rover.getLastTelemetryMessage() != null) idParaRegistar = -1;
                    else idParaRegistar = givenID;

                    reply = new Message(
                            receivedMsg.getSequenceNumber()+1,
                            ackNum,
                            Message.MessageDataTypes.ROVER_INIT,
                            new RoverInitMessage(idParaRegistar)
                    );
                    break;

                // --- PEDIDO DE MISSÃO (Com Cache) ---
                case REQUEST_MISSION:
                    RequestMission req = (RequestMission) receivedMsg.getMessageData();

                    System.out.println("[Mothership] Choosing a new mission for Rover " + req.getIdRover());
                    Mission mission = this.mothershipMissions.getMission();

                    if (mission == null) {
                        // Tratar erro ou enviar espera
                        break;
                    }
                    mission.setRoverId(req.getIdRover());
                    mothershipMissions.startMission(mission);
                    // Nota: O sequenceNumber deve vir do servidor (localSequenceNumber++)
                    // O ackNum deve vir calculado corretamente (Seq + Len)
                    reply = new Message(
                            receivedMsg.getSequenceNumber()+1,
                            ackNum,
                            Message.MessageDataTypes.MISSION,
                            new MissionMessage(mission));
                    break;

                default:
                    break;
            }

            if (reply == null) {
                reply = new Message(
                        receivedMsg.getSequenceNumber()+1,
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
        public Set<Integer> getRoverIDs() {
            return this.rovers.keySet();
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
                RoverTelemetryMessage msg = i.getLastTelemetryMessage();
                if (msg != null) res.add(msg);
            }
            return res;
        }

        // retorna info de um rover especifico, por ID
        public RoverInfo getRoverById(int id) {
            return this.rovers.get(id);
        }
    }