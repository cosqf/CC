package Mothership;

public class Mothership { // controller
    public static void main(String[] args) {
        try {
            Thread udpServer = new Thread(new MissionLinkServer(5000));
            Thread tcpServer = new Thread(new TelemetryStreamServer(6000));

            udpServer.start();
            tcpServer.start();

            System.out.println("Mothership is running MissionLink (UDP) on port 1000 and TelemetryStream (TCP) on port 2000.");
        } catch (Exception e) {
            System.out.println("Failed to establish connections: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

