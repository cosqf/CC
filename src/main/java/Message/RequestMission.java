package Message;

import java.io.*;

public class RequestMission implements MessageData {
    private final int idRover;

    public RequestMission (int idRover) {
        this.idRover = idRover;
    }

    public int getIdRover() {
        return this.idRover;
    }
    // ----------------------------

    @Override
    public byte[] convertMessageDataToBytes() {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(byteOut)) {
            out.writeInt(4);
            out.writeInt(idRover);
            out.flush();
            return byteOut.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Serialization failed for RequestMission.", e);
        }
    }

    public static RequestMission convertBytesToMessageData(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bis)) {
            dis.readInt();
            int id = dis.readInt();
            return new RequestMission(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "RequestMission { " + "idRover = " + idRover + '}';
    }
}
