package Rover;

import Connection.MissionLinkClient;
import Connection.NetworkConfig;
import Connection.TelemetryStreamClient;
import Message.Message;
import Message.RequestMission;
import Message.RoverTelemetryMessage;
import Message.RoverInitMessage;

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
        int ackToSend = 0;
        RequestMission req = new RequestMission(this.rover.getId());
        Message msg = new Message(
                seq,
                ackToSend,
                Message.MessageDataTypes.REQUEST_MISSION,
                req
        );

        int payloadSize = req.convertMessageDataToBytes().length;
        this.localSequenceNumber += (payloadSize > 0 ? payloadSize : 1);
        missionLinkClient.enqueueMessage(msg);
        System.out.println("[Rover " + this.rover.getId() + "] sent request (Seq " + seq + ", Ack " + ackToSend + ").");
    }
    public void sendInit() {

        int seq = this.localSequenceNumber;
        int ackToSend = 0;
        RoverInitMessage init = new RoverInitMessage(this.rover.getId());

        Message msg = new Message(
                seq,
                ackToSend,
                Message.MessageDataTypes.ROVER_INIT,
                init
        );

        int size = init.convertMessageDataToBytes().length;
        this.localSequenceNumber += (size > 0 ? size : 1);
        missionLinkClient.enqueueMessage(msg);
        System.out.println("[Rover] sent an init message (Seq " + seq + ").");

        // 7. (Opcional) Bloquear até receber o ID da Nave
        // Como o Sender garante a entrega, só precisamos de esperar que o Receiver processe a resposta.
        /*
        while (this.rover.getId() == -1) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("[Rover] ID atribuído e confirmado: " + this.rover.getId());
        */
    }

    public void sendTelemetry() {
        Thread t = new Thread (() -> {
            boolean running = true;
            while (running) {
                try {
                    Message msg = new Message(0, Message.MessageDataTypes.ROVER_TELEMETRY, new RoverTelemetryMessage(this.rover));
                    telemetryStreamClient.enqueueMessage(msg);
                    System.out.println("[Rover " + this.rover.getId() + "] sent a telemetry message.");
                    Thread.sleep(120000); // every 2 minutes
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
            e.printStackTrace();
        }
    }
}