package Message;

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
        byte[] bytes = new byte[2];
        bytes[0] = (byte) 1;
        bytes[1] = (byte) idRover;
        return bytes;
    }

    public static RequestMission convertBytesToMessageData(byte[] bytes) {
        return new RequestMission(bytes[1]);
    }

    @Override
    public String toString() {
        return "RequestMission { " + "idRover = " + idRover + '}';
    }
}
