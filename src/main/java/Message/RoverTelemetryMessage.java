package Message;

import Rover.PhysicalState;
import Rover.Rover;
import Utils.Point3D;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RoverTelemetryMessage implements MessageData{
    private int id;
    private Point3D position;
    private Rover.MissionState state;
    private double batteryLevel;
    private List<String> inventory;
    private List <PhysicalState> physicalStates;

    public RoverTelemetryMessage (Rover rover){
        this.id = rover.getId();
        this.position = rover.getPosition();
        this.state = rover.getState();
        this.physicalStates = rover.getPhysicalStates();
        this.physicalStates = rover.getPhysicalStates();
        this.batteryLevel = rover.getBatteryLevel();
        this.inventory = rover.getInventory();
    }

    public RoverTelemetryMessage (int id,  Point3D position, Rover.MissionState missionState, double batteryLevel, List<String> inventory, List <PhysicalState> physicalStates) {
        this.id = id;
        this.position = position;
        this.state = missionState;
        this.batteryLevel = batteryLevel;
        this.inventory = inventory;
        this.physicalStates = physicalStates;
    }

    public int getId() {
        return id;
    }

    public Point3D getPosition() {
        return position;
    }

    public Rover.MissionState getMissionState() {
        return state;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public List<String> getInventory() {
        return inventory;
    }

    public List<PhysicalState> getPhysicalStates() {
        return physicalStates;
    }

    @Override
    public byte[] convertMessageDataToBytes() {
        byte[] dataContentBytes;
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(byteOut)) {
                out.writeInt(id);
                out.writeDouble(position.x);
                out.writeDouble(position.y);
                out.writeDouble(position.z);
                out.write(state.ordinal());

                out.writeDouble(batteryLevel);

                out.writeInt(inventory.size());
                for (String item : inventory) {
                    byte[] nameBytes = item.getBytes(StandardCharsets.UTF_8);
                    out.writeInt(nameBytes.length);
                    out.write(nameBytes);
                }

                out.writeInt(physicalStates.size());
                for (PhysicalState ps : physicalStates) {
                    byte[] psBytes = ps.toByteArray();
                    out.writeInt(psBytes.length);
                    out.write(psBytes);
                }
                dataContentBytes = byteOut.toByteArray();
            }
            return MessageData.addSizeToArray(dataContentBytes);
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

        int totalLength = buffer.getInt();
        int id = buffer.getInt();
        double x = buffer.getDouble();
        double y = buffer.getDouble();
        double z = buffer.getDouble();
        int stateOrdinal = buffer.get();
        double batteryLevel = buffer.getDouble();

        int invCount = buffer.getInt();
        List<String> inventory = new ArrayList<>();
        for (int i = 0; i < invCount; i++) {
            int len = buffer.getInt();
            byte[] strBytes = new byte[len];
            buffer.get(strBytes);
            inventory.add(new String(strBytes, StandardCharsets.UTF_8));
        }

        int psCount = buffer.getInt();
        List<PhysicalState> physicalStates = new ArrayList<>();
        for (int i = 0; i < psCount; i++) {
            int len = buffer.getInt();
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

    public String toStringForAPI() {
        final int WIDTH = 80;
        final String SEPARATOR_LINE = "+" + "-".repeat(WIDTH - 2) + "+" + "\n";
        StringBuilder sb = new StringBuilder();

        double roundedBattery = Math.round(this.batteryLevel * 100.0) / 100.0;

        sb.append(SEPARATOR_LINE);
        sb.append(String.format("| Rover %d:%-" + (WIDTH - 11 - ((int) Math.log10(Math.abs(this.id)) + 1)) + "s |\n", this.id, ""));
        sb.append(SEPARATOR_LINE);
        sb.append(String.format("| Position -> %-" + (WIDTH - 16) + "s |\n", this.position.toString()));
        sb.append(String.format("| Status -> %-" + (WIDTH - 14) + "s |\n", this.state.toString()));
        sb.append(String.format("| Battery -> %-" + (WIDTH - 15) + "s |\n", roundedBattery + "%"));

        appendWrappedLine(sb, "Inventory -> ", printInventory(), WIDTH);
        
        appendWrappedLine(sb, "Physical Status -> ", printPhysicalStates(), WIDTH);

        sb.append(SEPARATOR_LINE);
        return sb.toString();
    }

    private void appendWrappedLine(StringBuilder sb, String label, String text, int width) {
        int diff = width - 4 - label.length();
        
        List<String> lines = wrapText(text, diff);
        sb.append(String.format("| %s%-" + diff + "s |\n", label, lines.get(0)));

        String indent = " ".repeat(label.length());
        for (int i = 1; i < lines.size(); i++) {
            sb.append(String.format("| %s%-" + diff + "s |\n", indent, lines.get(i)));
        }
    }
    
    private List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= maxLength) {
                currentLine.append(" ").append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }



    public String printInventory() {
        return String.join(", ", this.inventory);
    }

    public String printPhysicalStates() {
        return String.join(", ", this.physicalStates.stream()
                .map(PhysicalState::toString)
                .toList());
    }

}
