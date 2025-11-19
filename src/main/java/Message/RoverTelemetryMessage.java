package Message;

import Rover.PhysicalState;
import Rover.Rover;
import Utils.Point3D;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RoverTelemetryMessage implements MessageData{
    public int id;
    public Point3D position;
    public Rover.MissionState state;
    public int batteryLevel;
    public List<String> inventory;
    public List <PhysicalState> physicalStates;

    public RoverTelemetryMessage (Rover rover){
        this.id = rover.getId();
        this.position = rover.getPosition();
        this.state = rover.getState();
        this.physicalStates = rover.getPhysicalStates();
        this.physicalStates = rover.getPhysicalStates();
        this.batteryLevel = rover.getBatteryLevel();
        this.inventory = rover.getInventory();
    }

    public RoverTelemetryMessage (int id,  Point3D position, Rover.MissionState missionState, int batteryLevel, List<String> inventory, List <PhysicalState> physicalStates) {
        this.id = id;
        this.position = position;
        this.state = missionState;
        this.batteryLevel = batteryLevel;
        this.inventory = inventory;
        this.physicalStates = physicalStates;
    }

    @Override
    public byte[] convertMessageDataToBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            out.write((byte) id);
            out.write((byte) position.x);
            out.write((byte) position.y);
            out.write((byte) position.z);
            out.write((byte) state.ordinal());
            out.write((byte) batteryLevel);

            out.write((byte) inventory.size());
            for (String item : inventory) {
                byte[] nameBytes = item.getBytes(StandardCharsets.UTF_8);
                out.write((byte) nameBytes.length);
                out.write(nameBytes);
            }

            out.write((byte) physicalStates.size());
            for (PhysicalState ps : physicalStates) {
                byte[] psBytes = ps.toByteArray();
                out.write((byte) psBytes.length);
                out.write(psBytes);
            }

            byte[] bytes = out.toByteArray();

            byte[] bytesWithLength = new byte[bytes.length+1];
            bytesWithLength[0] = (byte) out.size();
            System.arraycopy(bytes, 0, bytesWithLength, 1, bytes.length);

            return bytesWithLength;

        } catch (Exception e) {
            throw new RuntimeException("Error converting message to bytes", e);
        }
    }
        /*
    example
        id = 1
        position = (10, 20, 30)
        state = IN_MISSION (0)
        battery = 85
        inventory = ["rock", "sample"]
        physicalStates = [("camera1", 30)]

    in the array would be
        [1][10][20][30][0][85]      <-- fixed
        [2][4]["rock"][6]["sample"] <-- size varies
        [1][8]["camera1"][30]       <--/

     */

    public static RoverTelemetryMessage convertBytesToMessageData(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int totalLength = Byte.toUnsignedInt(buffer.get());
        int id = Byte.toUnsignedInt(buffer.get());
        int x = Byte.toUnsignedInt(buffer.get());
        int y = Byte.toUnsignedInt(buffer.get());
        int z = Byte.toUnsignedInt(buffer.get());
        int stateOrdinal = Byte.toUnsignedInt(buffer.get());
        int batteryLevel = Byte.toUnsignedInt(buffer.get());

        int invCount = Byte.toUnsignedInt(buffer.get());
        List<String> inventory = new ArrayList<>();
        for (int i = 0; i < invCount; i++) {
            int len = Byte.toUnsignedInt(buffer.get());
            byte[] strBytes = new byte[len];
            buffer.get(strBytes);
            inventory.add(new String(strBytes, StandardCharsets.UTF_8));
        }

        int psCount = Byte.toUnsignedInt(buffer.get());
        List<PhysicalState> physicalStates = new ArrayList<>();
        for (int i = 0; i < psCount; i++) {
            int len = Byte.toUnsignedInt(buffer.get());
            byte[] psBytes = new byte[len];
            buffer.get(psBytes);
            physicalStates.add(PhysicalState.fromBytes(psBytes));
        }

        return new RoverTelemetryMessage(
                id, new Point3D(x,y,z), Rover.MissionState.values()[stateOrdinal],
                batteryLevel, inventory, physicalStates);
    }

    @Override
    public String toString() {
        return "RoverTelemetryMessage { " +
                "id = " + id +
                ", position = " + position +
                ", state = " + state +
                ", batteryLevel = " + batteryLevel +
                ", inventory = " + inventory +
                ", physicalStates = " + physicalStates +
                '}';
    }
}
