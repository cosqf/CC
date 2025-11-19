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

    public RoverConnection(Rover rover) {
        this.rover = rover;
    }

    public void requestMission() {
        Message msg = new Message(0, Message.MessageDataTypes.REQUEST_MISSION, new RequestMission(this.rover.getId()));
        missionLinkClient.enqueueMessage(msg);
        System.out.println("[Rover " + this.rover.getId() + "] sent a mission request.");
    }
    public void sendInit() {
        Message msg = new Message(0, Message.MessageDataTypes.ROVER_INIT, new RoverInitMessage());
        missionLinkClient.enqueueMessage(msg);
        System.out.println("[Rover] sent an init message.");
        // NEEDS TO BLOCK UNTIL IT RECEIVES REPLY
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