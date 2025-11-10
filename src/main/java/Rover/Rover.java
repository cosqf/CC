package Rover;
import Connection.MissionLinkClient;
import Connection.NetworkConfig;
import Connection.TelemetryStreamClient;
import Utils.Point3D;

import java.util.ArrayList;
import java.util.List;

public class Rover {
    private int id;
    private Point3D position;
    private MissionState state = MissionState.IDLE;
    private int batteryLevel = 100;
    private List<String> inventory = new ArrayList<>();
    private List <PhysicalState> physicalStates;

    public enum MissionState {
        IN_MISSION,
        IDLE,
        CHARGING,
        ERROR,
        ON_THE_WAY
    }

    public Rover (int id){
        this.id = id;
        this.position = new Point3D(0,0,0);
        this.physicalStates = new ArrayList<>();
    }

    public Rover(int id, Point3D position, List<PhysicalState> physicalStates) {
        this.id = id;
        this.position = position;
        this.physicalStates = new ArrayList<>();
        this.physicalStates.addAll(physicalStates);
    }

    public int getId() {
        return id;
    }
    public Point3D getPosition() {return position;}
    public MissionState getState() {return state;}
    public int getBatteryLevel() {return batteryLevel;}
    public List <String> getInventory() {
        return new ArrayList<>(inventory);
    }
    public List <PhysicalState> getPhysicalStates() {
        return new ArrayList<>(physicalStates);
    }

    public void setId(int id) {
        this.id = id;
    }
    public void setPosition(Point3D position) {this.position = position;}
    public void setState(MissionState state) {this.state = state;}
    public void setBatteryLevel(int batteryLevel) {this.batteryLevel = batteryLevel;}

    public static void main(String[] args) {
        NetworkConfig networkConfig = new NetworkConfig();
        String mothership_id = networkConfig.getIp(NetworkConfig.ID.MOTHERSHIP_IP);
        String ml_port = networkConfig.getIp(NetworkConfig.ID.MISSION_LINK_PORT);
        String ts_port = networkConfig.getIp(NetworkConfig.ID.TELEMETRY_STREAM_PORT);

        try {
            Thread udpThread = new Thread(new MissionLinkClient(mothership_id, Integer.parseInt(ml_port)));
            Thread tcpThread = new Thread(new TelemetryStreamClient(mothership_id, Integer.parseInt(ts_port)));

            udpThread.start();
            tcpThread.start();

            System.out.println("Rover online: MissionLink (UDP) + TelemetryStream (TCP) active");
        } catch (Exception e) {
            System.out.println("Failed to establish connections: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
