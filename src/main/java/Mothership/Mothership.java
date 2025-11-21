package Mothership;

import Message.*;
import Utils.Point3D;
import Mission.Mission;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Mothership { // controller
    // Mapa para guardar os Rovers (evita IndexOutOfBounds)
    private Map<Integer, RoverInfo> rovers = new HashMap<>();
    private int localSequenceNumber = 0;

    public void updateRoverInfoWithTelemetry(Message msg) {

        if (msg.getMessageDataType() != Message.MessageDataTypes.ROVER_TELEMETRY) return;

        RoverTelemetryMessage telemetry = (RoverTelemetryMessage) msg.getMessageData();
        int roverId = telemetry.id;
        RoverInfo roverInfo = this.rovers.get(roverId);

        if (roverInfo == null) {
            System.out.println("[Mothership] Novo Rover detetado via Telemetria: ID " + roverId);
            // Ajusta os argumentos conforme o teu construtor de RoverInfo (3 ou 4 args)
            roverInfo = new RoverInfo(roverId, null, 0);
            this.rovers.put(roverId, roverInfo);
        }

        roverInfo.updateLastTelemetryMessage(telemetry);
        roverInfo.updateLastActiveTimestamp(System.currentTimeMillis());
        System.out.println("[Mothership] Telemetria atualizada para Rover " + roverId);
    }

    public static void main(String[] args) {
        Mothership mothership = new Mothership();
        MothershipConnection connection = new MothershipConnection(mothership);
        connection.startServer();
    }

    public Message generateReply(Message receivedMsg) {
        Message reply = null;

        // 1. CALCULAR O ACK
        int payloadSize = 0;
        if (receivedMsg.getMessageData() != null) {
            payloadSize = receivedMsg.getMessageData().convertMessageDataToBytes().length;
        }
        int ackNum = receivedMsg.getSequenceNumber() + (payloadSize > 0 ? payloadSize : 1);

        switch (receivedMsg.getMessageDataType()) {
            case ACK:
                return null;

            case ROVER_INIT:
                RoverInitMessage initMsg = (RoverInitMessage) receivedMsg.getMessageData();
                int idParaRegistar;

                // 1. Decidir qual o ID
                if (initMsg.id > 0) {
                    idParaRegistar = initMsg.id;
                } else {
                    idParaRegistar = rovers.size() + 1;
                }

                // 2. LÓGICA DE PROTEÇÃO (IDEMPOTÊNCIA)
                // Se o Rover já existe, NÃO criamos novo registo, apenas preparamos a resposta.
                if (!rovers.containsKey(idParaRegistar)) {
                    System.out.println("[Mothership] A registar NOVO Rover: " + idParaRegistar);
                    RoverInfo rInfo = new RoverInfo(idParaRegistar, null, 0);
                    rovers.put(idParaRegistar, rInfo);
                } else {
                    System.out.println("[Mothership] Rover " + idParaRegistar + " já existe. A reenviar confirmação.");
                }

                // 3. Responder sempre com o mesmo ID
                reply = new Message(
                        this.localSequenceNumber++,
                        ackNum,
                        Message.MessageDataTypes.ROVER_INIT,
                        new RoverInitMessage(idParaRegistar)
                );
                break;

            case REQUEST_MISSION:
                RequestMission req = (RequestMission) receivedMsg.getMessageData();
                RoverInfo rInfo = this.rovers.get(req.getIdRover());

                // Filtro de Duplicados
                if (rInfo != null && receivedMsg.getSequenceNumber() <= rInfo.getLastProcessedSequenceNumber()) {
                    System.out.println("[Mothership] Pedido duplicado ignorado (Seq " + receivedMsg.getSequenceNumber() + ").");
                    break;
                }
                if (rInfo != null) {
                    rInfo.setLastProcessedSequenceNumber(receivedMsg.getSequenceNumber());
                }

                // CORREÇÃO 2: Usar o ID correto na Missão
                // Antes tinhas: new Mission(1, ...) -> Isto forçava o ID a 1
                // Agora usamos: req.getIdRover()
                System.out.println("[Mothership] A criar Missão para Rover " + req.getIdRover());

                Mission novaMissao = new Mission(
                        req.getIdRover(), // <--- ID DINÂMICO AQUI
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
                break;

            default:
                break;
        }

        // Fallback: ACK puro
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

}