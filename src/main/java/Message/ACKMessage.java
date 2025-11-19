package Message;

public class ACKMessage implements MessageData {
    private final int noSequence;
    public ACKMessage(int noSequence) {
        this.noSequence = noSequence;
    }

    @Override
    public byte[] convertMessageDataToBytes() {
        byte[] bytes = new byte[2];
        bytes[0] = 1;
        bytes[1] = (byte) noSequence;
        return bytes;
    }

    public static ACKMessage convertBytesToMessageData(byte[] bytes) {
        int noSequence = bytes[1];
        return new ACKMessage(noSequence);
    }

    public int getNoSequence() {
        return noSequence;
    }

    @Override
    public String toString() {
        return "ACK = { seq = " + noSequence + " }";
    }
}
