package Message;

public class RoverInitMessage implements MessageData{
    public int id = -1;

    public RoverInitMessage () {}

    public RoverInitMessage (int id) {
        this.id = id;
    }
    @Override
    public byte[] convertMessageDataToBytes() {
        byte[] bytes  = new byte[2];
        bytes[0] = (byte) 1;
        bytes[1] = (byte) id;
        return bytes;
    }

    public static RoverInitMessage convertBytesToMessageData(byte[] bytes) {
        int id = bytes[1];
        if (id == -1) return new RoverInitMessage();
        return new RoverInitMessage(id);
    }

    @Override
    public String toString() {
        return "RoverInitMessage { " +
                "id = " + id +
                '}';
    }
}
