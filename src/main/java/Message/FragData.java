package Message;

import java.util.Arrays;

public class FragData implements MessageData {
    private final byte[] dataChunk;

    public FragData(byte[] dataChunk) {
        this.dataChunk = dataChunk;
    }

    public byte[] getDataChunk() {
        return dataChunk;
    }

    @Override
    public byte[] convertMessageDataToBytes() {
        return dataChunk;
    }

    @Override
    public String toString() {
        String content = Arrays.toString(dataChunk);

        if (content.length() > 50) {
            content = content.substring(0, 47) + "...]";
        }

        return "FragData { Size=" + dataChunk.length + ", Bytes=" + content + " }";
    }
}