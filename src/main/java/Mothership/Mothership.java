package Mothership;

import Connection.MissionLinkServer;
import Connection.TelemetryStreamServer;
import Connection.NetworkConfig;

public class Mothership { // controller
    public static void main(String[] args) {
        NetworkConfig networkConfig = new NetworkConfig();
        String ml_port = networkConfig.getIp(NetworkConfig.ID.MISSION_LINK_PORT);
        String ts_port = networkConfig.getIp(NetworkConfig.ID.TELEMETRY_STREAM_PORT);
        try {
            Thread udpServer = new Thread(new MissionLinkServer(Integer.parseInt(ml_port)));
            Thread tcpServer = new Thread(new TelemetryStreamServer(Integer.parseInt(ts_port)));

            udpServer.start();
            tcpServer.start();

            System.out.println("Mothership is running MissionLink (UDP) on port " + ml_port + " and TelemetryStream (TCP) on port " + ts_port + ".");
        } catch (Exception e) {
            System.out.println("Failed to establish connections: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

