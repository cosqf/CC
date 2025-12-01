package Rover;

import Connection.MissionLinkClient;
import Connection.NetworkConfig;
import Connection.TelemetryStreamClient;
import Message.MessageUDP;
import Message.Message;
import Message.RequestMission;
import Message.UpdateMission;
import Message.RoverTelemetryMessage;
import Message.RoverInitMessage;
import Mission.Mission;

import java.util.Arrays;

public class RoverConnection {
    private final Rover rover;
    private MissionLinkClient missionLinkClient;
    private TelemetryStreamClient telemetryStreamClient;
    private int localSequenceNumber = 0;
    public RoverConnection(Rover rover) {
        this.rover = rover;
    }

    public void requestMission() {
        int seq = this.localSequenceNumber;
        int ackToSend = -1;
        RequestMission req = new RequestMission(this.rover.getId());
        MessageUDP msg = new MessageUDP(
                seq,
                ackToSend, // O Ack vem em segundo lugar!
                0, 0, 1,   // Fragmentação (0, 0, 1) vem a seguir
                MessageUDP.MessageDataTypes.REQUEST_MISSION,
                req
        );

        int payloadSize = req.convertMessageDataToBytes().length;
        this.localSequenceNumber += (payloadSize > 0 ? payloadSize : 1);
        missionLinkClient.enqueueMessage(msg);
        System.out.println("[Rover " + this.rover.getId() + "] sent mission request (Seq " + seq + ", Ack " + ackToSend + ").");
    }

    public void discardMission(Mission m, int idRover) {
        int seq = this.localSequenceNumber;
        int ackToSend = -1;
        UpdateMission req = new UpdateMission(m.getMissionId(),idRover,-1);
        MessageUDP msg = new MessageUDP(
                seq,
                ackToSend,
                0, 0, 1,
                MessageUDP.MessageDataTypes.MISSION_UPDATE,
                req
        );

        int payloadSize = req.convertMessageDataToBytes().length;
        this.localSequenceNumber += (payloadSize > 0 ? payloadSize : 1);
        missionLinkClient.enqueueMessage(msg);
        System.out.println("[Rover " + this.rover.getId() + "] discarded mission " + m.getMissionId() + " (Seq " + seq + ", Ack " + ackToSend + ").");
    }

    public void sendInit(int roverId) {
        int seq = this.localSequenceNumber;
        int ackToSend = 0;
        RoverInitMessage init = new RoverInitMessage(roverId);

        MessageUDP msg = new MessageUDP(
                seq,
                ackToSend,
                0, 0, 1,
                MessageUDP.MessageDataTypes.ROVER_INIT,
                init
        );

        int size = init.convertMessageDataToBytes().length;
        this.localSequenceNumber += (size > 0 ? size : 1);
        missionLinkClient.enqueueMessage(msg);
        System.out.println("[Rover] sent an init message (Seq " + seq + ").");
        System.out.println("msg " + msg);
        System.out.println("data " + Arrays.toString(msg.getMessageData().convertMessageDataToBytes()));
        System.out.println(Arrays.toString(msg.convertMessageToBytes()));
    }

    public void sendUpdateMission (UpdateMission updateMission) {
        MessageUDP msg = new MessageUDP(0, -1, 0, 0, 1,MessageUDP.MessageDataTypes.MISSION_UPDATE, updateMission);
        missionLinkClient.enqueueMessage(msg);
        System.out.println("[Rover " + this.rover.getId() + "] sent a mission update.");
    }

    public void sendTelemetry() {
        Thread t = new Thread (() -> {
            boolean running = true;
            while (running) {
                try {
                    Message msg = new Message(0, Message.MessageDataTypes.ROVER_TELEMETRY, new RoverTelemetryMessage(this.rover));
                    telemetryStreamClient.enqueueMessage(msg);
                    System.out.println("[Rover " + this.rover.getId() + "] sent a telemetry message.");
                    Thread.sleep(30000); // every 30 seconds
                } catch (InterruptedException e) {
                    System.out.println("[Rover " + this.rover.getId() + "] Connection thread interrupted.");
                    running = false;
                }
                catch (TelemetryStreamClient.TelemetryStreamNotRunning e) {
                    System.out.println("[Rover " + this.rover.getId() + "] TS not running.");
                    running = false;
                }
            }
        });
        t.start();
    }

    public void connectServer() {
        NetworkConfig networkConfig = new NetworkConfig();
        String mothership_ip = networkConfig.getIp(NetworkConfig.ID.MOTHERSHIP_IP);
        String ml_port = networkConfig.getIp(NetworkConfig.ID.MISSION_LINK_PORT);
        String ts_port = networkConfig.getIp(NetworkConfig.ID.TELEMETRY_STREAM_PORT);

        try {
            this.missionLinkClient = new MissionLinkClient(mothership_ip, Integer.parseInt(ml_port), this.rover);
            Thread udpThread = new Thread(this.missionLinkClient);

            this.telemetryStreamClient = new TelemetryStreamClient(mothership_ip, Integer.parseInt(ts_port));
            Thread tcpThread = new Thread(this.telemetryStreamClient);

            udpThread.start();
            tcpThread.start();

            System.out.println("Rover online: MissionLink (UDP) + TelemetryStream (TCP) active");
        } catch (Exception e) {
            System.out.println("Failed to establish connections: " + e.getMessage());
        }
    }
    public void closeServer() {
        missionLinkClient.stop();
        telemetryStreamClient.stop();
    }
}