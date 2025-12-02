package Mothership;

import API.APIServer;
import Connection.MissionLinkServer;
import Connection.NetworkConfig;
import Connection.TelemetryStreamServer;
import Message.Message;
import Message.MissionMessage;
import Connection.FragManager;

public class MothershipConnection {
    private MissionLinkServer missionLinkServer;
    private TelemetryStreamServer telemetryStreamServer;
    private APIServer observationAPI;
    private Mothership mothership;

    public MothershipConnection(Mothership mothership) {
        this.mothership = mothership;
    }

    public void sendMission (MissionMessage missionMessage) {
        Message msg = new Message(0, Message.MessageDataTypes.MISSION, missionMessage);

        int roverID = missionMessage.getRoverId();
        RoverInfo r = mothership.getRoverById(roverID);
        missionLinkServer.enqueueMessage(msg, r.getRoverIpAddress(), r.getRoverPort());
        System.out.println("[MOTHERSHIP] sent a mission to Rover " + roverID + ".");
    }

    public void startServer() {
        NetworkConfig networkConfig = new NetworkConfig();

        String ml_port = networkConfig.getIp(NetworkConfig.ID.MISSION_LINK_PORT);
        String ts_port = networkConfig.getIp(NetworkConfig.ID.TELEMETRY_STREAM_PORT);
        String api_port = networkConfig.getIp(NetworkConfig.ID.API_SERVER);
        String ms_ip = networkConfig.getIp(NetworkConfig.ID.MOTHERSHIP_IP);

        try {
            FragManager.MAX_FRAGMENT_SIZE = 4;

            missionLinkServer = new MissionLinkServer(Integer.parseInt(ml_port), mothership);
            Thread udpServer = new Thread(missionLinkServer);

            telemetryStreamServer = new TelemetryStreamServer(Integer.parseInt(ts_port), mothership);
            Thread tcpServer = new Thread(telemetryStreamServer);

            observationAPI = new APIServer(ms_ip, Integer.parseInt(api_port), mothership);
            Thread apiServer = new Thread(observationAPI);

            udpServer.start();
            tcpServer.start();
            apiServer.start();
        } catch (Exception e) {
            System.out.println("Failed to establish connections: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
