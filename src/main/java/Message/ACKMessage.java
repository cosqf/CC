package Message;

import java.io.*;

public class ACKMessage implements MessageData {
    private final int noSequence;
    public ACKMessage(int noSequence) {
        this.noSequence = noSequence;
    }

    @Override
    public byte[] convertMessageDataToBytes() {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(byteOut)) {
            out.writeInt(4);
            out.writeInt(noSequence);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteOut.toByteArray();
    }

    public static ACKMessage convertBytesToMessageData(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bis)) {
            dis.readInt();
            int sequence = dis.readInt();
            return new ACKMessage(sequence);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getNoSequence() {
        return noSequence;
    }

    @Override
    public String toString() {
        return "ACK = { seq = " + noSequence + " }";
    }
}
