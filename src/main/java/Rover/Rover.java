package Rover;

import Utils.Point3D;

import Message.*;
import Message.Message.MessageDataTypes;


import java.util.ArrayList;
import java.util.List;

public class Rover {
    private int id;
    private Point3D position;
    private MissionState state = MissionState.IDLE;
    private double batteryLevel = 100;
    private final Base base;
    private final List<String> inventory = new ArrayList<>();
    private final int maxInventorySpace;
    private final List <PhysicalState> physicalStates;

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

        int roverId = 1; // Valor default caso te esqueças de passar argumento

        if (args.length > 0) {
            try {
                // O argumento vem como String ("1"), temos de converter para int
                // args[0] é o primeiro argumento da linha de comandos
                roverId = Integer.parseInt(args[0].trim().replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                System.err.println("Erro: O ID do Rover deve ser um número inteiro.");
                return;
            }
        } else {
            System.out.println("Warning: No ID given. Using default ID: 1");
        }

        Rover rover = new Rover(roverId, new Point3D(0,0,0), physicalStates, 5);
        rover.roverConnection.connectServer();
        rover.roverConnection.sendInit();
        rover.roverMissions.run();
        rover.roverConnection.sendTelemetry();
    }

    public void processMessage(MessageDataTypes type, MessageData msg) {
        switch (type) {
            case ROVER_INIT:
                RoverInitMessage roverMsg = (RoverInitMessage) msg;
                setId(roverMsg.getId());
                break;
            case MISSION:
                MissionMessage missionMsg = (MissionMessage) msg;
                this.roverMissions.addMission(missionMsg.getMission());
                break;
            default:
                break;
        }
    }

    public Message generateReply(Message receivedMsg) {
        Message reply = null;

        switch (receivedMsg.getMessageDataType()) {
            case MISSION:
                // reply = ....
                break;
            case ACK:
                return null; // ACKs dont need any reply
            default:
                break;
        }

        if (reply == null) { // no reply needed, send an ACK only
            reply = new Message(receivedMsg.getSequenceNumber()+1,
                    receivedMsg.getMessageId(),
                    Message.MessageDataTypes.ACK,
                    new ACKMessage(receivedMsg.getSequenceNumber())
            );
        }

        return reply;
    }
    public void sendUpdateMission (UpdateMission mission) {
        this.roverConnection.sendUpdateMission(mission);
    }
}