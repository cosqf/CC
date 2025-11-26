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
    private int id;
    private Point3D position;
    private Rover.MissionState state;
    private int batteryLevel;
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

    public RoverTelemetryMessage (int id,  Point3D position, Rover.MissionState missionState, int batteryLevel, List<String> inventory, List <PhysicalState> physicalStates) {
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

    public int getBatteryLevel() {
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

    public String toStringForAPI() {
        final int WIDTH = 80;
        final int CONTENT_WIDTH = WIDTH - 4; // Space for content between "| " and " |"

        final String SEPARATOR_LINE = "+" + "-".repeat(WIDTH - 2) + "+";
        final String DATA_SEPARATOR = "|" + "-".repeat(WIDTH - 2) + "|";
        StringBuilder sb = new StringBuilder();

        // --- Fixed lines ---
        sb.append(SEPARATOR_LINE).append("\n");

        String roverLine = String.format("| Rover %d:%-" + (CONTENT_WIDTH - ("Rover " + this.id + ":").length()) + "s |", this.id, "");
        sb.append(roverLine).append("\n");

        sb.append(DATA_SEPARATOR).append("\n");

        String positionLine = String.format("| Position -> %-" + (CONTENT_WIDTH - "Position -> ".length()) + "s |", this.position.toString());
        String statusLine = String.format("| Status -> %-" + (CONTENT_WIDTH - "Status -> ".length()) + "s |", this.state.toString());
        String batteryLine = String.format("| Battery -> %-" + (CONTENT_WIDTH - "Battery -> ".length()) + "s |", this.batteryLevel + "%");

        sb.append(positionLine).append("\n");
        sb.append(statusLine).append("\n");
        sb.append(batteryLine).append("\n");

        // --- Inventory Output (wrapped) ---
        String inventoryLabel = "Inventory -> ";
        List<String> inventoryLines = wrapText(this.printInventory(), CONTENT_WIDTH - inventoryLabel.length());

        // First line with label
        sb.append("| ").append(inventoryLabel)
                .append(String.format("%-" + (CONTENT_WIDTH - inventoryLabel.length()) + "s", inventoryLines.get(0)))
                .append(" |\n");

        // Continuation lines
        for (int i = 1; i < inventoryLines.size(); i++) {
            sb.append("| ").append(String.format("%-" + CONTENT_WIDTH + "s", inventoryLines.get(i))).append(" |\n");
        }

        // --- Physical Status Output (wrapped) ---
        String physicalLabel = "Physical Status -> ";
        List<String> physicalLines = wrapText(this.printPhysicalStates(), CONTENT_WIDTH - physicalLabel.length());

        // First line with label
        sb.append("| ").append(physicalLabel)
                .append(String.format("%-" + (CONTENT_WIDTH - physicalLabel.length()) + "s", physicalLines.get(0)))
                .append(" |\n");

        // Continuation lines
        for (int i = 1; i < physicalLines.size(); i++) {
            sb.append("| ").append(String.format("%-" + CONTENT_WIDTH + "s", physicalLines.get(i))).append(" |\n");
        }

        sb.append(SEPARATOR_LINE);

        return sb.toString();
    }

    private List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split(" "); // split by spaces
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= maxLength) {
                currentLine.append(" ").append(word);
            } else {
                // line is full, start new line
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }



    public String printInventory() {
        StringBuilder sb = new StringBuilder();
        for (String i : this.inventory) {
            sb.append(i).append(", "); // Use comma-space separator instead of newline
        }
        // Remove the final ", " if it exists
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }

    public String printPhysicalStates() {
        StringBuilder sb = new StringBuilder();
        for (PhysicalState ps : this.physicalStates) {
            sb.append(ps.toString()).append(", "); // Use comma-space separator
        }
        // Remove the final ", " if it exists
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }
}
