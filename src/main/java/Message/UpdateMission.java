package Message;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class UpdateMission implements MessageData {
    private final int idMission;
    private final int idRover;
    private final int completionLevel;
    private final byte[] extraData;

    public UpdateMission (int idMission, int idRover, int completionLevel) {
        this.idMission = idMission;
        this.idRover = idRover;
        this.completionLevel = completionLevel;
        this.extraData = new byte[0];
    }
    public UpdateMission (int idMission, int idRover, int completionLevel, byte[] extraData) {
        this.idMission = idMission;
        this.idRover = idRover;
        this.completionLevel = completionLevel;
        this.extraData = extraData;
    }

    @Override
    public byte[] convertMessageDataToBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            out.write((byte) idMission);
            out.write((byte) idRover);
            out.write((byte) completionLevel);
            out.write((byte) extraData.length);
            out.write(extraData);

            byte[] bytes =  out.toByteArray();

            byte[] bytesWithLength = new byte[bytes.length+1];
            bytesWithLength[0] = (byte) bytes.length;
            System.arraycopy(bytes, 0, bytesWithLength, 1, bytes.length);

            return bytesWithLength;
        } catch (Exception e) {
            throw new RuntimeException("Error converting message to bytes", e);
        }
    }

    public static UpdateMission convertBytesToMessageData(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int totalLength = Byte.toUnsignedInt(buffer.get());
        int idMission = Byte.toUnsignedInt(buffer.get());
        int idRover = Byte.toUnsignedInt(buffer.get());
        int completionLevel = Byte.toUnsignedInt(buffer.get());

        int extraDataLength = Byte.toUnsignedInt(buffer.get());
        byte[] extraData = new byte[extraDataLength];
        for (int i = 0; i < extraDataLength; i++) {
            extraData[i]=buffer.get();
        }

        return new UpdateMission(idMission, idRover, completionLevel, extraData);
    }
}
