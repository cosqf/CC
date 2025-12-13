package Message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UpdateMission implements MessageData {
    private final int idMission;
    private final int idRover;
    private final int completionLevel;
    private final byte[] extraData;

    public int getIdMission() {
        return idMission;
    }
    public int getCompletionLevel(){
        return completionLevel;
    }

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
        byte[] dataContentBytes;
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteOut)) {
                out.writeInt(idMission);
                out.writeInt(idRover);
                out.writeInt(completionLevel);
                out.writeInt(extraData.length);
                out.write(extraData);

                byteOut.flush();
                dataContentBytes = byteOut.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error converting message to bytes", e);
        }
        return MessageData.addSizeToArray(dataContentBytes);
    }

    public static UpdateMission convertBytesToMessageData(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int totalLength = buffer.getInt();
        int idMission = buffer.getInt();
        int idRover = buffer.getInt();
        int completionLevel = buffer.getInt();

        int extraDataLength = buffer.getInt();
        byte[] extraData = new byte[extraDataLength];
        for (int i = 0; i < extraDataLength; i++) {
            extraData[i]=buffer.get();
        }

        return new UpdateMission(idMission, idRover, completionLevel, extraData);
    }

    @Override
    public String toString() {
        String res = "UpdateMission { " +
                    "idMission = " + idMission +
                    ", idRover = " + idRover +
                    ", completionLevel = " + completionLevel +
                    ", extraData = ";

        if (extraData.length > 50) {
            String content = Arrays.toString(extraData);
            content = content.substring(0, 47) + "...]";
            res = res.concat(content);
        }
        else res = res.concat(Arrays.toString(extraData));
        res = res.concat (" }");
        return res;
    }
}
