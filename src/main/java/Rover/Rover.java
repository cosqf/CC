package Rover;
import Message.MessageData;
import Message.UpdateMission;
import Utils.Point3D;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Rover {
    private int id;
    private Point3D position;
    private MissionState state = MissionState.IDLE;
    private int batteryLevel = 100;
    private List<String> inventory = new ArrayList<>();
    private List <PhysicalState> physicalStates;
    private static final int VARIABLES_IN_MESSAGE = 8; // position counts as 3, plus length

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

    public void Connect () throws IOException {
        try {
            Socket socket = new Socket("localhost", 12345);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream()); //existe uma opção autoFlush que força o envio dos inputs

            String fromMothership;
            while ((fromMothership = in.readLine()) != null) {
                //faz coisas
            }

            socket.shutdownOutput();
            socket.shutdownInput();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
