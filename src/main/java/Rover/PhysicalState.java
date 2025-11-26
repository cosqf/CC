package Rover;

import java.nio.charset.StandardCharsets;

public class PhysicalState {
    String partName; // eg camera
    int condition; // 30 (percent), depletes with time

    public PhysicalState (String partName, int condition) {
        this.partName = partName;
        this.condition = condition;
    }

    public byte[] toByteArray () {
        byte[] nameBytes = partName.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[nameBytes.length + 1];

        System.arraycopy(nameBytes, 0, bytes, 0, nameBytes.length);
        bytes[bytes.length - 1] = (byte) condition;
        return bytes;
    }

    public static PhysicalState fromBytes(byte[] bytes) {
        int condition = bytes[bytes.length - 1] & 0xFF; // hex means its unsigned

        String partName = new String(bytes, 0, bytes.length - 1, StandardCharsets.UTF_8);

        PhysicalState ps = new PhysicalState(partName, condition);
        return ps;
    }
    @Override
    public String toString () {
        return partName + ": " + condition + "%";
    }
}
