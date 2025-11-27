package Message;

import Rover.PhysicalState;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class RoverInitMessage implements MessageData{
    private int id = -1;

    public RoverInitMessage () {}

    public RoverInitMessage (int id) {
        this.id = id;
    }
    @Override
    public byte[] convertMessageDataToBytes() {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(byteOut)) {
            out.writeInt(4);
            out.writeInt(id);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteOut.toByteArray();
    }

    public int getId() {return id;}
    public static RoverInitMessage convertBytesToMessageData(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bis)) {
            dis.readInt();
            int id = dis.readInt();
            if (id == -1) return new RoverInitMessage();
            return new RoverInitMessage(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "RoverInitMessage { " +
                "id = " + id +
                '}';
    }
}
