package Rover;

import Message.*;
import Message.Message.MessageDataTypes;
import Utils.Point3D;
import Utils.UDPPrint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Rover {
    private int id;
    private Point3D position;
    private MissionState state = MissionState.IDLE;
    private double batteryLevel = 100;
    private final Base base;
    private final List<String> inventory = new ArrayList<>();
    private final int maxInventorySpace;
    private final List <PhysicalState> physicalStates;
    private final CountDownLatch initiatedLatch = new CountDownLatch(1);

    private final RoverMissions roverMissions;
    private final RoverConnection roverConnection;

    public enum MissionState {
        IN_MISSION,
        IDLE,
        CHARGING,
        ERROR,
        ON_THE_WAY
    }

    public Rover(int id, Point3D position, List<PhysicalState> physicalStates, int inventorySpace) {
        this.id = id;
        this.position = position;
        this.base = new Base(position);
        this.physicalStates = new ArrayList<>();
        this.physicalStates.addAll(physicalStates);
        this.maxInventorySpace = inventorySpace;
        this.roverConnection = new RoverConnection(this);
        this.roverMissions = new RoverMissions(this, roverConnection);
    }

    public int getId() {
        return id;
    }
    public Point3D getPosition() {return position;}
    public MissionState getState() {return state;}
    public double getBatteryLevel() {return batteryLevel;}
    public Base getBase() {return base;}
    public List <String> getInventory() {
        return new ArrayList<>(inventory);
    }
    public List <PhysicalState> getPhysicalStates() {
        return new ArrayList<>(physicalStates);
    }
    public int getMaxInventorySpace() {
        return this.maxInventorySpace;
    }

    public void setId(int id) {
        this.id = id;
    }
    public void setPosition(Point3D position) {this.position = position;}
    public void setState(MissionState state) {this.state = state;}
    public void setBatteryLevel(double batteryLevel) {this.batteryLevel = batteryLevel;}
    public void addToInventory(String item) {
        if (maxInventorySpace > inventory.size()) inventory.add(item);
    }
    public void clearInventory () {this.inventory.clear();}

    public static void main(String[] args) throws InterruptedException {
        ArrayList<PhysicalState> physicalStates = new ArrayList<>();
        physicalStates.add(new PhysicalState("wheels", 100));
        physicalStates.add(new PhysicalState("camera", 80));

        int roverId = 1; //default value in case no argument is passed

        if (args.length > 0) {
            try {
                roverId = Integer.parseInt(args[0].trim().replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                System.err.println("Warning: The ID for the Rover should be an int.");
                return;
            }
        } else {
            System.out.println("Warning: No ID given. Using default ID: " + roverId);
        }

        Rover rover = new Rover(roverId, new Point3D(0,0,0), physicalStates, 5);
        rover.roverConnection.connectServer();
        rover.roverConnection.sendInit(roverId);

        try {
            rover.initiatedLatch.await(); // wait here until signal is received
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (rover.getId() == -1) {
            System.out.println("A rover with that ID is already active. Shutting down.");
            rover.roverConnection.closeServer();
            return;
        }
        UDPPrint.logSuccess("RCV", null, "ID Assigned: " +rover.getId());
        rover.roverMissions.run();
        rover.roverConnection.sendTelemetry();
    }

    public void processMessage(MessageDataTypes type, MessageData msg) {
        switch (type) {
            case ROVER_INIT:
                RoverInitMessage roverMsg = (RoverInitMessage) msg;
                if (initiatedLatch.getCount() == 0 && this.id == roverMsg.getId()) return;

                setId(roverMsg.getId());
                initiatedLatch.countDown();
                break;
            case MISSION:
                MissionMessage missionMsg = (MissionMessage) msg;
                this.roverMissions.addMission(missionMsg.getMission());
                break;
            default:
                break;
        }
    }

    public MessageUDP generateReply(MessageUDP receivedMsg, int ackNum) {
        if (receivedMsg.getMessageDataType() == MessageDataTypes.ACK) {
            return null; // ACKs dont need any reply
        }

        // no reply needed, send an ACK only
        return new MessageUDP(
                0,
                ackNum,
                0, 0, 1,
                MessageDataTypes.ACK,
                new ACKMessage(receivedMsg.getSequenceNumber())
        );
    }
    public void sendUpdateMission (UpdateMission mission) {
        this.roverConnection.sendUpdateMission(mission);
    }
}